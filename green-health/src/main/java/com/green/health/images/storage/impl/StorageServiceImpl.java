package com.green.health.images.storage.impl;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.green.health.images.storage.StorageService;
import com.green.health.util.RestPreconditions;
import com.green.health.util.exceptions.MyRestPreconditionsException;

@Service
public class StorageServiceImpl implements StorageService {

	private String fileStorageLocation;
	
	@Autowired
	public StorageServiceImpl() throws MyRestPreconditionsException {
		this.fileStorageLocation = System.getProperty("user.dir")+File.separator+"images"+File.separator;
		
		try {
            Files.createDirectories(Paths.get(fileStorageLocation).toAbsolutePath().normalize());
        } catch (Exception ex) {
            throw new MyRestPreconditionsException("Image service initialization error", 
            		"Could not create the directory where the uploaded files will be stored.");
        }
	}
	
	@Override
	public void deleteImage(final Long id, final boolean isUser) throws MyRestPreconditionsException {
		String dirPath = buildDirPath(id);
		
		RestPreconditions.assertTrue((new File(dirPath)).exists(), 
				"Delete Image Error", "No directory exists for that id.");
		
		if(isUser){
			deletePreviousImage(dirPath, "profile_THUMBNAIL");
		} else {
			deletePreviousImage(dirPath, "herb_THUMBNAIL");
			deletePreviousImage(dirPath, "herb");
		}
		
		deleteEmptyDirectoryTreeLeaf(dirPath);
	}
	
	private void deleteEmptyDirectoryTreeLeaf(final String dirPath) throws MyRestPreconditionsException {
		File dir = new File(dirPath);
		// dirPath was already checked.
		
		// if directory is empty :
		if(dir.list().length == 0) {
			// delete empty dir and check :
			RestPreconditions.assertTrue(dir.delete(),"Image Storage Service error", "Failed to delete empty directory leaf "+dirPath);
			// make new dir path :
			String newDirPath = dirPath.split("[^0-9][0-9]$")[0];
			// recursive call - check if parent dir is empty too and delete it accordingly
			deleteEmptyDirectoryTreeLeaf(newDirPath);
		}
	}
	
	public ResponseEntity<Resource> getImage(final Long id, final String name) throws MyRestPreconditionsException {
	    
	    byte[] data = Base64.getEncoder().encodeToString(readImage(id, name)).getBytes();
	    
	    if(data.length>0) {
		    return ResponseEntity.ok()
		            .contentType(MediaType.parseMediaType("application/octet-stream"))
		            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + name + "\"")
		            .contentLength(data.length)
		            .body(new ByteArrayResource(data));
		} else {
			return new ResponseEntity<Resource>(null, null, HttpStatus.NO_CONTENT);
		}
	}

	@Override
	public void saveImage(final MultipartFile mpf, final Long id, final boolean isUser) throws MyRestPreconditionsException {	
		RestPreconditions.assertTrue(null != mpf, "Save image error", "You are attempting to upload a non-existing file.");
		RestPreconditions.assertTrue(!mpf.isEmpty(), "Save image error", "You are attempting to upload an empty file.");
		RestPreconditions.assertTrue(!mpf.getOriginalFilename().contains(".."), "Save image error", "Upload filename contains invalid path sequence");
		RestPreconditions.assertTrue(checkMpFileExtension(mpf.getOriginalFilename()), "Save image error", "You are only allowed to upload files with extensions jpg, jpeg, png and bmp");
		
		// compose dir structure :
		String path = buildDirPath(id);
		
		File dir = new File(path);
		
		// if dir does not exist yet, make it :
		if(!dir.exists()){
			try {
				dir.mkdirs();
			} catch (Exception e) {
				throw new MyRestPreconditionsException("Save image error", e.getMessage());
			}
		}
		
		// check that it is writeable
		RestPreconditions.assertTrue(java.nio.file.Files.isWritable(dir.toPath()), 
				"Directory creation error", "File location is not writable");
		
		// put files in dir :
		saveFileInDir(path, mpf, isUser);
	}
	
	private byte[] readImage(final Long id, final String name) throws MyRestPreconditionsException{
		
		String imgDir = buildDirPath(id);
		
		{
			File tmp = new File(imgDir);
			RestPreconditions.assertTrue(tmp.exists(), 
					"Image read error","File path "+imgDir+" does not exist");
			RestPreconditions.assertTrue(java.nio.file.Files.isReadable(tmp.toPath()), 
					"Image read error", "File path "+imgDir+" is not readable");
		} // tmp ceases to exist here.
		
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(imgDir))){
			Path filePath = null;
			
			for (Path path : directoryStream) {
            	if(path.toString().contains(name)){
            		filePath = path;
            		break;
            	}
            }
			
			if(filePath!=null){
				return Files.readAllBytes(filePath);
			} else {
            	return new byte[0];
            }
		} catch (Exception e){
			throw new MyRestPreconditionsException("Resource retrieve error", "Oops ! Something went wrong in retrieving the image.");
		}
	}
	
	// save file :
	private void saveFileInDir(final String dir, final MultipartFile mpf, final boolean isUser) throws MyRestPreconditionsException{

		String fileName = isUser ? "profile" : "herb";
		
		String contentType;
		{//in case there is more than one . in name
			String parts[] = mpf.getOriginalFilename().split("\\.");
			contentType = parts[parts.length-1];
		}// parts[] ceases to exist here
		
		// delete previous (has to be here - even if filenames are same, extensions can differ)
		deletePreviousImage(dir, fileName);
		
		try {
			// save original :
			if(!isUser){ // herb has both regular size image and thumbnail ; user has only thumbnail :
				Files.copy(mpf.getInputStream(), 
						Paths.get(dir + fileName+"."+contentType).toAbsolutePath().normalize(), // targetLocation
						StandardCopyOption.REPLACE_EXISTING);
			}
			// save thumbnail :
			Files.copy(scaleImageInputstream(mpf, contentType, 80, 80), 
					Paths.get(dir + fileName+"_THUMBNAIL."+contentType).toAbsolutePath().normalize(), // targetLocation
					StandardCopyOption.REPLACE_EXISTING);
			
		}catch (IllegalStateException | IOException e) {
			e.printStackTrace();
			throw new MyRestPreconditionsException("Image save error", "Failed to save image in "+dir);
		}
	}
	
	private void deletePreviousImage(final String dir, final String name) throws MyRestPreconditionsException{
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(dir))) {
            for (Path path : directoryStream) {
            	if(path.toString().contains(name)){
            		Files.delete(path);
            		break;
            	}
            }
        } catch (IOException ex) {
        	throw new MyRestPreconditionsException("Delete image error",ex.getMessage());
        }
	}
	
	private boolean checkMpFileExtension(final String fileName){
		return Pattern.compile("[.](jpg)|(JPG)|(png)|(PNG)|(bmp)|(BMP)|(jpeg)|(JPEG)$").matcher(fileName).find();
	}
	
	private InputStream scaleImageInputstream(final MultipartFile multipart,final String contentType,final int targetWidth,final int targetHeight) throws MyRestPreconditionsException {
		try{
	        BufferedImage scaledImg = Scalr.resize(ImageIO.read(multipart.getInputStream()), 
	        									   Scalr.Method.QUALITY, Scalr.Mode.FIT_TO_WIDTH,
	                targetWidth, targetHeight, Scalr.OP_ANTIALIAS);
	        
	        ByteArrayOutputStream os = new ByteArrayOutputStream();
	        ImageIO.write(scaledImg, contentType, os);
	        os.flush();
	        return new ByteArrayInputStream(os.toByteArray());
		}
		catch(Exception e){
			throw new MyRestPreconditionsException("Image scaling error","Ooops - something went wrong !");
		}
    }

	// create dir path in classpath: from userId / herbId
	private String buildDirPath(final Long id) throws MyRestPreconditionsException{
		
		char[] folders = (""+id).toCharArray();
		
		String dir = fileStorageLocation;
		
		for(int i=folders.length-1 ; i>=0 ; i--){
			dir += (folders[i]+File.separator);
		}
		
		return dir;
	}
}
