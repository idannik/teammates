<%@ tag description="Instructor feedback submission form for both instructorFeedbackSubmissionEdit and instructorFeedbackQuestionSubmissionEdit" %>
<%@ tag import="teammates.common.util.Const"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/shared/feedbackSubmissionEdit" prefix="feedbackSubmissionEdit" %>

<%@ attribute name="feedbackSubmissionForm" type="teammates.ui.controller.FeedbackSubmissionEditPageData" required="true" %>

<form method="post" name="form-submit-response" action="${feedbackSubmissionForm.submitAction}">
    <jsp:include page="<%= Const.ViewURIs.FEEDBACK_SUBMISSION_EDIT %>" />
    
    <div class="bold align-center"> 
        <c:if test="${feedbackSubmissionForm.moderation}">       
            <input name="moderatedperson" value="${feedbackSubmissionForm.previewInstructor.email}" type="hidden">
        </c:if>
        
        <c:choose>
            <c:when test="${empty feedbackSubmissionForm.bundle.questionResponseBundle}">
                    There are no questions for you to answer here!
            </c:when>
            <c:otherwise>
                <input type="submit" class="btn btn-primary center-block"
                       id="response-submit-button" data-toggle="tooltip"
                       data-placement="top" title="<%= Const.Tooltips.FEEDBACK_SESSION_EDIT_SAVE %>"
                       value="Submit Feedback"
                       <c:if test="${feedbackSubmissionForm.preview or (not feedbackSubmissionForm.submittable)}">
                           disabled
                       </c:if>>
            </c:otherwise>
        </c:choose>
    </div>
    <br> 
    <br>
</form>