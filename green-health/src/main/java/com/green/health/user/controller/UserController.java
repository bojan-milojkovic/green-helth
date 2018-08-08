package com.green.health.user.controller;

import java.security.Principal;
import java.util.List;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.green.health.user.entities.MiniUserDTO;
import com.green.health.user.entities.UserDTO;
import com.green.health.user.service.UserService;
import com.green.health.util.exceptions.MyRestPreconditionsException;

@Controller
@RequestMapping(value="/users")
public class UserController {

	@Autowired
	private UserService userServiceImpl;
	
	// .../gh/users
	@RequestMapping(value = "", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasRole('ROLE_USER')")
	@ResponseStatus(HttpStatus.OK)
	public @ResponseBody List<UserDTO> getAllUsers(){
		return userServiceImpl.getAll();
	}
	
	// .../gh/users/3
	@RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasRole('ROLE_USER')")
	@ResponseStatus(HttpStatus.OK)
	public @ResponseBody UserDTO getUserById(@PathVariable("id") Long id){
		return userServiceImpl.getUserById(id);
	}
	
	// .../gh/users/ue?username=Koala2&email=blatruc@gmail.com
	@RequestMapping(value = "/ue", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasRole('ROLE_USER')")
	@ResponseStatus(HttpStatus.OK)
	public @ResponseBody UserDTO getUserByUsernameOrEmail(@RequestParam(value="username", required=false) String username,
														  @RequestParam(value="email", required=false) String email) 
														  throws MyRestPreconditionsException{
		return userServiceImpl.getUserByUsernameOrEmail(username, email);
	}	
	
	
	
	// .../gh/users
	// @Valid triggers the MyControllerAdvice when UserJPA is invalid
	@RequestMapping(value = "", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public void saveNewUser(@RequestBody @Valid UserDTO model) throws MyRestPreconditionsException {
		userServiceImpl.addUserToDb(model);
	}
	
	// .../gh/users/3
	@RequestMapping(value="/{id}", method = RequestMethod.PATCH, consumes = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasRole('ROLE_USER')")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public @ResponseBody UserDTO editUserById(@RequestBody @Valid UserDTO model, @PathVariable("id") Long id, Principal principal) throws MyRestPreconditionsException {
		model.setUsername(principal.getName());
		return userServiceImpl.editUser(model, id);
	}
	
	// .../gh/users/3
	@RequestMapping(value="/{id}", method = RequestMethod.DELETE)
	@PreAuthorize("hasRole('ROLE_USER')")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void deleteUserById(@PathVariable("id") Long id, Principal principal) throws MyRestPreconditionsException {
		userServiceImpl.deleteUser(id, principal.getName());
	}
	
	// .../gh/users/cpw
	@RequestMapping(value="/cpw/{id}", method = RequestMethod.PATCH, consumes = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasRole('ROLE_USER')")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void changePassword(@RequestBody @Valid MiniUserDTO model, @PathVariable("id") Long id, Principal principal) throws MyRestPreconditionsException {
		model.setId(id);
		userServiceImpl.changePassword(model, principal.getName());
	}
}