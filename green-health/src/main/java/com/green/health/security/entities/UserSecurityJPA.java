package com.green.health.security.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.MapsId;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.green.health.ratings.entities.LinkJPA;
import com.green.health.user.entities.UserJPA;

@Entity
@Table(name="user_security")
public class UserSecurityJPA {
	
	@Id
	@Column (name="user_id")
	private Long id;
	
	@Column (name="username")
	private String username;
	
	@Column(name="password")
	private String password;
	
	@OneToOne(fetch=FetchType.LAZY)
	@JoinColumn(name = "user_id")
	@MapsId
	private UserJPA userJpa;
	
	@OneToMany(mappedBy="userSecurityJpa", fetch=FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
	private List<UserHasRolesJPA> userHasRolesJpa = new ArrayList<UserHasRolesJPA>();
	
	@Column(name="active")
	private Boolean active;

	@Column(name="not_locked")
	private Boolean notLocked;
	
	@Column(name="last_login")
	private LocalDateTime lastLogin;

	@Column(name="last_password_change")
	private LocalDateTime lastPasswordChange;
	
	@Column(name="last_update")
	private LocalDateTime lastUpdate;
	
	@ManyToMany(mappedBy="raters")
	private Set<LinkJPA> links = new HashSet<LinkJPA>();
	
	public LocalDateTime getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(LocalDateTime lastUpdate) {
		this.lastUpdate = lastUpdate;
	}


	public UserSecurityJPA(){
		this.setLastLogin(LocalDateTime.now());
		this.setLastPasswordChange(LocalDateTime.now());
		this.setLastUpdate(LocalDateTime.now());
	}
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Boolean isActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}
	
	public Boolean getActive() {
		return active;
	}

	public Boolean isNotLocked() {
		return notLocked;
	}
	
	public Boolean getNotLocked() {
		return notLocked;
	}

	public void setNotLocked(Boolean notLocked) {
		this.notLocked = notLocked;
	}

	public LocalDateTime getLastLogin() {
		return lastLogin;
	}

	public void setLastLogin(LocalDateTime lastLogin) {
		this.lastLogin = lastLogin;
	}

	public LocalDateTime getLastPasswordChange() {
		return lastPasswordChange;
	}

	public void setLastPasswordChange(LocalDateTime lastPasswordChange) {
		this.lastPasswordChange = lastPasswordChange;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public UserJPA getUserJpa() {
		return userJpa;
	}

	public void setUserJpa(UserJPA userJpa) {
		this.userJpa = userJpa;
	}

	public List<UserHasRolesJPA> getUserHasRolesJpa() {
		return userHasRolesJpa;
	}

	public void setUserHasRolesJpa(List<UserHasRolesJPA> userHasRolesJpa) {
		this.userHasRolesJpa = userHasRolesJpa;
	}

	public Set<LinkJPA> getLinks() {
		return links;
	}

	public void setLinks(Set<LinkJPA> links) {
		this.links = links;
	}
}