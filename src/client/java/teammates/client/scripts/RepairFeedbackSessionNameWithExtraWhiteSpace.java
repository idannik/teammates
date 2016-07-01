package teammates.client.scripts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import teammates.client.remoteapi.RemoteApiClient;
import teammates.common.datatransfer.FeedbackResponseAttributes;
import teammates.common.datatransfer.FeedbackResponseCommentAttributes;
import teammates.common.datatransfer.FeedbackSessionAttributes;
import teammates.common.exception.EntityAlreadyExistsException;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.exception.InvalidParametersException;
import teammates.common.util.StringHelper;
import teammates.storage.api.FeedbackResponseCommentsDb;
import teammates.storage.api.FeedbackResponsesDb;
import teammates.storage.api.FeedbackSessionsDb;
import teammates.storage.datastore.Datastore;
import teammates.storage.entity.FeedbackQuestion;
import teammates.storage.entity.FeedbackResponse;
import teammates.storage.entity.FeedbackResponseComment;
import teammates.storage.entity.FeedbackSession;

/**
 * Previews and removes extra spaces in FeedbackSessionAttributes, FeedbackResponseAttributes,
 * FeedbackQuestionAttributes and FeedbackResponseCommentAttribute.
 */
public class RepairFeedbackSessionNameWithExtraWhiteSpace extends RemoteApiClient {
    private static final boolean isPreview = false;
    
    private FeedbackSessionsDb feedbackSessionsDb = new FeedbackSessionsDb();
    private FeedbackResponsesDb feedbackResponsesDb = new FeedbackResponsesDb();
    private FeedbackResponseCommentsDb feedbackResponseCommentsDb = new FeedbackResponseCommentsDb();
    
    private List<String> courseIdsToRunOn = Arrays.asList();
    
    public static void main(String[] args) throws IOException {
        RepairFeedbackSessionNameWithExtraWhiteSpace migrator = new RepairFeedbackSessionNameWithExtraWhiteSpace();
        migrator.doOperationRemotely();
    }

    @Override
    protected void doOperation() {
        Datastore.initialize();
        
        List<FeedbackSession> feedbackSessions;
        if (courseIdsToRunOn.isEmpty()) {
            feedbackSessions = getAllFeedbackSessionEntities();
        } else {
            feedbackSessions = getFeedbackSessionEntitiesOfCourses(courseIdsToRunOn);
        }
        
        System.out.println("There is/are " + feedbackSessions.size() + " session(s).");
        
        if (isPreview) {
            System.out.println("Checking extra spaces in feedback session name...");
        } else {
            System.out.println("Removing extra spaces in feedback session name...");
        }
        
        Set<String> coursesAffected = new HashSet<>();
        try {
            int numberOfFeedbackSessionWithExtraWhiteSpacesInName = 0;
            for (FeedbackSession session : feedbackSessions) {
                if (hasExtraSpaces(session.getFeedbackSessionName())) {
                    numberOfFeedbackSessionWithExtraWhiteSpacesInName++;
                    coursesAffected.add(session.getCourseId());
                    if (isPreview) {
                        showFeedbackSession(session);
                    } else {
                        fixFeedbackSession(session);
                    }
                }
            }
            
            if (isPreview) {
                System.out.println("There are/is " + numberOfFeedbackSessionWithExtraWhiteSpacesInName
                                   + "/" + feedbackSessions.size() + " feedback session(s) with extra spaces in name!");
            } else {
                System.out.println(numberOfFeedbackSessionWithExtraWhiteSpacesInName
                                   + "/" + feedbackSessions.size() + " feedback session(s) have been fixed!");
                System.out.println("Extra space removing done!");
            }
            
            if (!coursesAffected.isEmpty()) {
                printAffectedCourses(coursesAffected);
            }
        } catch (InvalidParametersException | EntityDoesNotExistException | EntityAlreadyExistsException e) {
            e.printStackTrace();
        }
    }

    private void printAffectedCourses(Set<String> coursesAffected) {
        System.out.println("Courses Affected: ");
        StringBuilder coursesToRunOn = new StringBuilder();
        for (String course : coursesAffected) {
            coursesToRunOn.append(String.format("\"%s\", ", course));
        }
        System.out.println(coursesToRunOn.substring(0, coursesToRunOn.length() - 2));
    }

    /**
     * Displays the feedback session.
     */
    private void showFeedbackSession(FeedbackSession session) {
        System.out.println("Feedback Session Name: \"" + session.getFeedbackSessionName() + "\" in "
                           + session.getCourseId());
    }

    /**
     * Remove extra spaces in feedback session name of the feedback session and
     * related questions, responses and feedback response comments.
     */
    private void fixFeedbackSession(FeedbackSession session)
            throws InvalidParametersException, EntityDoesNotExistException, EntityAlreadyExistsException {
        fixFeedbackQuestionsOfFeedbackSession(session);
        fixFeedbackResponsesOfFeedbackSession(session);
        fixFeedbackResponseCommentsOfFeedbackSession(session);
        
        FeedbackSessionAttributes sessionAttribute = new FeedbackSessionAttributes(session);
        feedbackSessionsDb.deleteEntity(sessionAttribute);
        sessionAttribute.setFeedbackSessionName(
                StringHelper.removeExtraSpace(sessionAttribute.getFeedbackSessionName()));
        feedbackSessionsDb.createEntity(sessionAttribute);
    }

    /**
     * Removes extra space in feedbackSessionName in FeedbackResponseComments
     */
    private void fixFeedbackResponseCommentsOfFeedbackSession(FeedbackSession session)
            throws InvalidParametersException, EntityDoesNotExistException {
        List<FeedbackResponseCommentAttributes> feedbackResponseCommentList =
                feedbackResponseCommentsDb.getFeedbackResponseCommentsForSession(session.getCourseId(),
                                                                                 session.getFeedbackSessionName());
        List<FeedbackResponseComment> commentsToSave = new ArrayList<>();
        for (FeedbackResponseCommentAttributes comment : feedbackResponseCommentList) {
            comment.feedbackSessionName = StringHelper.removeExtraSpace(comment.feedbackSessionName);
            commentsToSave.add(comment.toEntity());
        }
        getPm().makePersistentAll(commentsToSave);
        getPm().flush();
    }

    /**
     * Removes extra space in feedbackSessionName in FeedbackResponses
     */
    private void fixFeedbackResponsesOfFeedbackSession(FeedbackSession session)
            throws InvalidParametersException, EntityDoesNotExistException {
        List<FeedbackResponseAttributes> feedbackResponseList =
                feedbackResponsesDb.getFeedbackResponsesForSession(
                        session.getFeedbackSessionName(), session.getCourseId());
        List<FeedbackResponse> responsesToSave = new ArrayList<>();
        for (FeedbackResponseAttributes response : feedbackResponseList) {
            response.feedbackSessionName = StringHelper.removeExtraSpace(response.feedbackSessionName);
            responsesToSave.add(response.toEntity());
        }
        
        getPm().makePersistentAll(responsesToSave);
        getPm().flush();
    }

    /**
     * Removes extra space in feedbackSessionName in FeedbackQuestions
     */
    private void fixFeedbackQuestionsOfFeedbackSession(FeedbackSession session)
            throws InvalidParametersException, EntityDoesNotExistException {
        Query q = getPm().newQuery(FeedbackQuestion.class);
        
        q.declareParameters("String feedbackSessionNameParam, String courseIdParam");
        q.setFilter("feedbackSessionName == feedbackSessionNameParam && "
                    + "courseId == courseIdParam");
        @SuppressWarnings("unchecked")
        List<FeedbackQuestion> questions = 
                (List<FeedbackQuestion>) q.execute(session.getFeedbackSessionName(), session.getCourseId());
        
        List<FeedbackQuestion> questionsToSave = new ArrayList<>();
        for (FeedbackQuestion question : questions) {
            question.setFeedbackSessionName(
                    StringHelper.removeExtraSpace(question.getFeedbackSessionName()));
            questionsToSave.add(question);
        }
        getPm().close();
    }
    
    /**
     * @return true if there is extra space in the string.
     */
    private boolean hasExtraSpaces(String s) {
        return !s.equals(StringHelper.removeExtraSpace(s));
    }
    
    protected PersistenceManager getPm() {
        return Datastore.getPersistenceManager();
    }
    
    @SuppressWarnings("unchecked")
    private List<FeedbackSession> getAllFeedbackSessionEntities() {
        Query q = getPm().newQuery(FeedbackSession.class);
        return (List<FeedbackSession>) q.execute();
    }
    
    @SuppressWarnings("unchecked")
    private List<FeedbackSession> getFeedbackSessionEntitiesOfCourses(List<String> courseIdsToRunOn) {
        List<FeedbackSession> feedbackSessions = new ArrayList<>();
        for (String course : courseIdsToRunOn) {
            Query q = getPm().newQuery(FeedbackSession.class);
            q.declareParameters("String courseIdParam");
            q.setFilter("courseId == courseIdParam");
            feedbackSessions.addAll((List<FeedbackSession>) q.execute(course));
        }
        return feedbackSessions;
    }
}
