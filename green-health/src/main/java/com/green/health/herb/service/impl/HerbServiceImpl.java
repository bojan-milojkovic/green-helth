package com.green.health.herb.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.green.health.herb.dao.HerbRepository;
import com.green.health.herb.entities.HerbDTO;
import com.green.health.herb.entities.HerbJPA;
import com.green.health.herb.service.HerbService;
import com.green.health.images.storage.StorageService;
import com.green.health.util.RestPreconditions;
import com.green.health.util.exceptions.MyRestPreconditionsException;

@Service
public class HerbServiceImpl implements HerbService {

	private HerbRepository herbDao;
	private StorageService storageServiceImpl;
	
	@Autowired
	public HerbServiceImpl(HerbRepository herbDao, StorageService storageServiceImpl){
		this.herbDao = herbDao;
		this.storageServiceImpl = storageServiceImpl;
	}
	
	@Override
	public List<HerbDTO> getAll() {
		return herbDao.findAll().stream().map(jpa -> convertJpaToModel(jpa)).collect(Collectors.toList());
	}
	
	@Override
	public HerbDTO getOneById(Long id) {
		return convertJpaToModel(herbDao.getOne(id));
	}

	@Override
	public HerbDTO getHerbBySrbName(String srbName) {
		return convertJpaToModel(herbDao.getHerbBySrbName(srbName));
	}

	@Override
	public HerbDTO getHerbByLatinName(String latinName) {
		return convertJpaToModel(herbDao.getHerbByLatinName(latinName));
	}

	@Override
	public void addNew(HerbDTO model) throws MyRestPreconditionsException {
		if(isPostDataPresent(model)) {
			// check that herb name is unique :
			RestPreconditions.checkSuchEntityAlreadyExists(herbDao.getHerbByLatinName(model.getLatinName()),
					"The herb with Latin name "+model.getLatinName()+" is already in our database.");
			RestPreconditions.checkSuchEntityAlreadyExists(herbDao.getHerbBySrbName(model.getSrbName()),
					"The herb with Serbian name "+model.getSrbName()+" is already in our database.");
			
			HerbJPA jpa = herbDao.save(convertModelToJPA(model));
			
			if(model.getImage()!=null){
				// save image :
				storageServiceImpl.saveImage(model.getImage(), jpa.getId(), false);
			}
		} else {
			MyRestPreconditionsException ex = new MyRestPreconditionsException("You cannot add this herb",
							"The following data is missing from the herb form");
			if(!RestPreconditions.checkStringMatches(model.getDescription(),"[A-Za-z0-9 .,:'()-]{10,}")){
				ex.getErrors().add("herb description");
			}
			if(!RestPreconditions.checkStringMatches(model.getGrowsAt(),"[A-Za-z0-9 .,:'()-]{10,}")){
				ex.getErrors().add("where the herb grows");
			}
			if(!RestPreconditions.checkStringMatches(model.getLatinName(),"[A-Za-z ]{3,}")){
				ex.getErrors().add("herb's latin name");
			}
			if(!RestPreconditions.checkStringMatches(model.getProperties(),"[A-Za-z0-9 .,:'()-]{10,}")){
				ex.getErrors().add("herb's use properties");
			}
			if(!RestPreconditions.checkStringMatches(model.getSrbName(),"[A-Za-z ]{3,}")){
				ex.getErrors().add("herb's Serbian name");
			}
			if(!RestPreconditions.checkStringMatches(model.getWarnings(),"[A-Za-z0-9 .,:'()-]{10,}")){
				ex.getErrors().add("herb's use warnings");
			}
			if(!RestPreconditions.checkStringMatches(model.getWhenToPick(),"[A-Za-z0-9 .,:'()-]{10,}")){
				ex.getErrors().add("when to pick the herb");
			}
			if(model.getWhereToBuy()!=null && !model.getWhereToBuy().matches("($[A-Za-z0-9 .,:'()-]{10,}^)|($^)")){
				ex.getErrors().add("where to buy the herb");
			}
			throw ex;
		}
	}

	@Override
	public HerbDTO edit(HerbDTO model, Long id) throws MyRestPreconditionsException {
		
		RestPreconditions.assertTrue(isPatchDataPresent(model), "Herb edit error", "Your herb edit request is invalid - You must provide some editable data");
		model.setId(id);
		HerbJPA jpa = convertModelToJPA(model);
		RestPreconditions.assertTrue(jpa!=null, "Herb edit error", "Herb with id = "+id+" does not exist in our database.");
		herbDao.save(jpa);
		return convertJpaToModel(jpa);
	}

	@Override
	public void delete(final Long id) throws MyRestPreconditionsException {
		RestPreconditions.assertTrue(herbDao.getOne(id)!=null, "Herb delete error",
					"Herb with id = "+ id + " does not exist in our database");
		herbDao.deleteById(id);
	}

	@Override
	public HerbJPA convertModelToJPA(HerbDTO model) {
		HerbJPA jpa = null;
		
		if(model.getId()==null){
			jpa = new HerbJPA();
			
			jpa.setDescription(model.getDescription());
			jpa.setGrowsAt(model.getGrowsAt());
			jpa.setLatinName(model.getLatinName());
			jpa.setSrbName(model.getSrbName());
			jpa.setProperties(model.getProperties());
			jpa.setWarnings(model.getWarnings());
			jpa.setWhenToPick(model.getWhenToPick());
			jpa.setWhereToBuy(model.getWhereToBuy());
		} else {
			jpa = herbDao.getOne(model.getId());
			if(jpa==null){ // in case of trying to edit a non existing herb 
				return null;
			}
			
			if(RestPreconditions.checkString(model.getDescription())){
				jpa.setDescription(model.getDescription());
			}
			if(RestPreconditions.checkString(model.getGrowsAt())){
				jpa.setGrowsAt(model.getGrowsAt());
			}
			if(RestPreconditions.checkString(model.getLatinName())){
				jpa.setLatinName(model.getLatinName());
			}
			if(RestPreconditions.checkString(model.getSrbName())){
				jpa.setSrbName(model.getSrbName());
			}
			if(RestPreconditions.checkString(model.getProperties())){
				jpa.setProperties(model.getProperties());
			}
			if(RestPreconditions.checkString(model.getWarnings())){
				jpa.setWarnings(model.getWarnings());
			}
			if(RestPreconditions.checkString(model.getWhenToPick())){
				jpa.setWhenToPick(model.getWhenToPick());
			}
			if(RestPreconditions.checkString(model.getWhereToBuy())){
				jpa.setWhereToBuy(model.getWhereToBuy());
			}
		}
		
		return jpa;
	}

	@Override
	public HerbDTO convertJpaToModel(HerbJPA jpa) {
		HerbDTO model = new HerbDTO();
		
		model.setDescription(jpa.getDescription());
		model.setGrowsAt(jpa.getGrowsAt());
		model.setId(jpa.getId());
		model.setLatinName(jpa.getLatinName());
		model.setSrbName(jpa.getSrbName());
		model.setProperties(jpa.getProperties());
		model.setWhenToPick(jpa.getWhenToPick());
		model.setWhereToBuy(jpa.getWhereToBuy());
		model.setWarnings(jpa.getWarnings());
		
		return model;
	}

	@Override
	public boolean isPostDataPresent(HerbDTO model) {
		return RestPreconditions.checkStringMatches(model.getDescription(),"[A-Za-z0-9 .,:'()-]{10,}") && 
				RestPreconditions.checkStringMatches(model.getGrowsAt(),"[A-Za-z0-9 .,:'()-]{10,}") && 
				RestPreconditions.checkStringMatches(model.getLatinName(),"[A-Za-z ]{3,}") &&
				RestPreconditions.checkStringMatches(model.getSrbName(),"[A-Za-z ]{3,}") && 
				RestPreconditions.checkStringMatches(model.getProperties(),"[A-Za-z0-9 .,:'()-]{10,}") && 
				RestPreconditions.checkStringMatches(model.getWarnings(),"[A-Za-z0-9 .,:'()-]{10,}") && 
				RestPreconditions.checkStringMatches(model.getWhenToPick(),"[A-Za-z0-9 .,:'()-]{10,}")/* && 
				RestPreconditions.checkString(model.getWhereToBuy())*/;
	}

	@Override
	public boolean isPatchDataPresent(HerbDTO model) {
		return RestPreconditions.checkStringMatches(model.getDescription(),"[A-Za-z0-9 .,:'()-]{10,}") ||
				RestPreconditions.checkStringMatches(model.getGrowsAt(),"[A-Za-z0-9 .,:'()-]{10,}") ||
				RestPreconditions.checkStringMatches(model.getLatinName(),"[A-Za-z ]{3,}") ||
				RestPreconditions.checkStringMatches(model.getSrbName(),"[A-Za-z ]{3,}") ||
				RestPreconditions.checkStringMatches(model.getProperties(),"[A-Za-z0-9 .,:'()-]{10,}") ||
				RestPreconditions.checkStringMatches(model.getWarnings(),"[A-Za-z0-9 .,:'()-]{10,}") ||
				RestPreconditions.checkStringMatches(model.getWhenToPick(),"[A-Za-z0-9 .,:'()-]{10,}") ||
				RestPreconditions.checkStringMatches(model.getWhereToBuy(),"[A-Za-z0-9 .,:'()-]{10,}");
	}
}