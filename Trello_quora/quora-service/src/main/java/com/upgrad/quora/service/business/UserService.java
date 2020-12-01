package com.upgrad.quora.service.business;

/* spring imports */
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/* app imports */
import com.upgrad.quora.service.dao.UserDao;
import com.upgrad.quora.service.entity.UserEntity;
import com.upgrad.quora.service.entity.UserAuthEntity;
import com.upgrad.quora.service.exception.SignUpRestrictedException;
import com.upgrad.quora.service.exception.AuthorizationFailedException;
import com.upgrad.quora.service.exception.AuthenticationFailedException;
import com.upgrad.quora.service.exception.SignOutRestrictedException;
import com.upgrad.quora.service.exception.UserNotFoundException;

/* java imports */
import javax.transaction.Transactional;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.Base64;

/** 
  * UserService.Class helps to handle all service details for users
  */

@Service
public class UserService {
  @Autowired
  private UserDao userDao;

  @Autowired
  private PasswordCryptographyProvider cryptor;
  
  /* performs the actual sign up process with the database */
  @Transactional
  public UserEntity performSignUp(UserEntity newUser, String userName, String email, String password) throws SignUpRestrictedException {
    /* if the username provided already exists in the current database */
    if(userDao.getUserByUserName(userName) != null){
      throw new SignUpRestrictedException("SGR-001","Try any other Username, this Username has already been taken");
    }
    /* if the email Id provided by the user already exists in the current database */
    else if(userDao.getUserByEmail(email) != null){
      throw new SignUpRestrictedException("SGR-002","This user has already been registered, try with any other emailId");
    }
    else {
      /* perform encryption for the password and generate the salt */
      char[] userPassword = password.toCharArray();
      String[] encrytedStuff = cryptor.encrypt(userPassword);
      String salt = encrytedStuff[0];
      String encPassword = encrytedStuff[1];

      /* update details for the newUser Entity */
      newUser.setPassword(encPassword);
      newUser.setSalt(salt);

      /* register and return as needed */
      UserEntity u = userDao.RegisterUser(newUser);
      if (u == null) {
        /* this is just in case for us to know when there is a failure - don;t remove it otherwise it may be difficult to 
         understand where the failure is happening! */
        throw new SignUpRestrictedException("SGR-003","DB Registration Error!");
      }
      else {
        return u;
      }
    }
  }
  
  /* user sign in service */
  @Transactional
  public UserAuthEntity performSignIn(String authorization) throws AuthenticationFailedException {
    /* 1. authorization details are sent through the headers. you'll need to get the username and password decoded */
    byte[] decode = Base64.getDecoder().decode(authorization.split("Basic ")[1]);
    String decodedText = new String(decode);
    String[] decodedArray = decodedText.split(":");
    String userName = decodedArray[0];
    String password = decodedArray[1];

    /* first check if the user name is registered or not */
    UserEntity user = userDao.getUserByUserName(userName);
    if (user == null) {
      throw new AuthenticationFailedException("ATH-001", "This username does not exist");
    }
    
    /* if you are here, it means that we have a registered user name with us in the database */
    String hashedPassword = cryptor.encrypt(password.toCharArray(), user.getSalt());
    if (!user.getPassword().equals(hashedPassword)) {
      throw new AuthenticationFailedException("ATH-002","Password failed");
    }
    else{
      /* we are good to go here... */
      final ZonedDateTime now = ZonedDateTime.now();
      final ZonedDateTime expiresAt = now.plusHours(8);
      JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(hashedPassword);

      /* build the user auth token object */
      UserAuthEntity userAuthTokenEntity = new UserAuthEntity();
      userAuthTokenEntity.setUser(user);
      userAuthTokenEntity.setUuid(user.getUuid());
      userAuthTokenEntity.setAccessToken(jwtTokenProvider.generateToken(user.getUuid(), now, expiresAt));
      userAuthTokenEntity.setLoginAt(now);
      userAuthTokenEntity.setExpiresAt(expiresAt);
      
      UserAuthEntity authedEntity = userDao.createAuthToken(userAuthTokenEntity);
      /* a back up exception that I am createing to keep a track of things! */
      if (authedEntity == null) {
        throw new AuthenticationFailedException("ATH-003","Could Not Create Auth Token");
      }
      else {
        userDao.updateUser(user);
        return userAuthTokenEntity;
      }
    }
  }

  /* user sign out service */
  @Transactional
  public UserEntity performSignOut(String userAuthToken) throws SignOutRestrictedException {
    UserAuthEntity userAuthEntity = userDao.findUserByThisAuthToken(userAuthToken);
    if (userAuthEntity == null) {
      throw new SignOutRestrictedException("SGR-001", "User is not Signed in");
    }
    else {
      userAuthEntity.setLogoutAt(ZonedDateTime.now());
      userDao.updateUser(userAuthEntity.getUser());
      return userAuthEntity.getUser();
    }
  }
  
  /* a single method that can be used to check if the auth token given is a valid token or not */
  public UserEntity performAuthTokenValidation(final String authTokenString) throws AuthorizationFailedException {
    UserAuthEntity userAuthEntity = userDao.findUserByThisAuthToken(authTokenString);
    if (userAuthEntity == null) {
      throw new AuthorizationFailedException("ATHR-001", "User has not signed in");
    }
    else if (userAuthEntity.getLogoutAt() != null) {
      throw new AuthorizationFailedException("ATHR-002", "User is signed out.Sign in first to get user details");
    }
    else {
      return userAuthEntity.getUser();
    }
  }

  /* helps to fetch a single user entity based on the id value recived from the controller */
  public UserEntity fetchUserById(final long id) throws UserNotFoundException {
    UserEntity fetchedEntity = userDao.fetchUserById(id);
    if (fetchedEntity == null) {
      throw new UserNotFoundException("USR-001", "User with entered uuid does not exist");
    }
    else {
      return fetchedEntity;
    }
  }

  /* helps to perform the delete request if we have a valid user entity that is registered with us */
  @Transactional
  public UserEntity performDelete(UserEntity admin, final long userId) 
  throws UserNotFoundException, AuthorizationFailedException {
    UserEntity userEntity = this.fetchUserById(userId);
    /* this is just a fallback */
    if (!admin.getRole().equals("admin")) {
      throw new AuthorizationFailedException("ATHR-003", "Unauthorized Access, Entered user is not an admin");
    }
    else {
      return userDao.performDelete(admin, userId);
    }
  }
}