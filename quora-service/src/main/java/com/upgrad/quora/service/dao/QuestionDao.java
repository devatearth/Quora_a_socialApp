package com.upgrad.quora.service.dao;

import com.upgrad.quora.service.entity.QuestionEntitiy;
import com.upgrad.quora.service.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.util.List;

@Repository
public class QuestionDao {

    //Creating an instance of EntityManager
    @Autowired
    private EntityManager entityManager;

    //Create a qusestion
    public QuestionEntitiy createQuestion(QuestionEntitiy questionEntitiy) {
        entityManager.persist(questionEntitiy);
        return questionEntitiy;
    }

    public List<QuestionEntitiy> getAllQuestions() {
        return entityManager.createNamedQuery("getAllQuestions", QuestionEntitiy.class).getResultList();
    }

    //Added by @github.com/vetrivel-muthusamy
    //Get Question from UUID
    public QuestionEntitiy getQuestionFromUuid(final String questionId) {
        try {
            return entityManager.createNamedQuery("getQuestionByUuid", QuestionEntitiy.class).setParameter("uuid", questionId).getSingleResult();

        } catch (NoResultException nre) {
            return null;
        }
    }


   public QuestionEntitiy updateQuestion(final QuestionEntitiy questionEntity) {
        return entityManager.merge(questionEntity);
    }

    public void deleteQuestion(final String uuid) {
        QuestionEntitiy questionEntity = getQuestionFromUuid(uuid);
        entityManager.remove(questionEntity);
    }

    public List<QuestionEntitiy> getAllQuestionsByUser(final UserEntity user) {
        try {
            return entityManager.createNamedQuery("getQuestionById", QuestionEntitiy.class).setParameter("user", user).getResultList();
        } catch (NoResultException nre) {
            return null;
        }
    }


}
