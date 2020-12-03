package com.upgrad.quora.service.business;

import com.upgrad.quora.service.dao.UserAuthDao;
import com.upgrad.quora.service.dao.UserDao;
import com.upgrad.quora.service.entity.UserAuthEntity;
import com.upgrad.quora.service.entity.UserEntity;
import com.upgrad.quora.service.exception.AuthorizationFailedException;
import com.upgrad.quora.service.exception.UserNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.ZonedDateTime;

//This class is implemented as AdminBusinessService class
@Service
public class AdminBusinessService {

    //Creating an instance to access database
    @Autowired
    private UserDao userDao;

    //Creating an instance to access DB
    @Autowired
    private UserAuthDao userAuthDao;
    /*This method will validate the access token
    @Parm -accessToken
    @Return - UserAuthEntity
     */
    @Transactional
    public UserAuthEntity ValidateAccessToken(String accessToken) throws AuthorizationFailedException {
        UserAuthEntity userAuthEntity = userAuthDao.getUserAuthEntityByAccessToken(accessToken);
        if (userAuthEntity == null) {
            throw new AuthorizationFailedException("ATHR-001", "User has not signed in");
        }
        System.out.println(userAuthEntity.getUuid());
        if (userAuthEntity.getLogoutAt() == null) {

            return userAuthEntity;
        }
        int difference = userAuthEntity.getLogoutAt().compareTo(ZonedDateTime.now());
        if (difference < 0) {
            throw new AuthorizationFailedException("ATHR-002", "User is signed out");
        }

        return userAuthEntity;
    }

    /*This method will delete the user
    @Parm - userId
    */
    @Transactional
    public String deleteUser(String userId) throws UserNotFoundException {
        UserEntity user = userDao.getUserByUserId(userId);
        if (user == null) {
            throw new UserNotFoundException("USR-001", "User with entered uuid to be deleted does not exist");
        }
        userDao.deleteUser(user);
        return "USER SUCCESSFULLY DELETED";
    }
}
