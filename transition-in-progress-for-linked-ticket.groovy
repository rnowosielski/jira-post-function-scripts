import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.workflow.WorkflowTransitionUtil;
import com.atlassian.jira.workflow.WorkflowTransitionUtilImpl;
import com.atlassian.jira.util.JiraUtils;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.user.ApplicationUser;
import org.apache.log4j.Category

def Category log = Category.getInstance("com.onresolve.jira.groovy.PostFunction")
log.setLevel(org.apache.log4j.Level.DEBUG)

String doneStatus = "Done";
String closedStatus = "Closed";
String inProgressStatus = "In Progress";
List<String> finalStatuses = [doneStatus, closedStatus]
String implementRelationship = "Implement"
int doneActionId = 31;
int progressActionId = 21;

def transitionTicketsImplementedByCurrentOne(int transitionId, List<String> finalStatuses, String implementRelationship, Category log) {
    log.debug "Entering transitionTicketsImplementedByCurrentOne()";
    try {
        def issueLinkManager = ComponentAccessor.getIssueLinkManager();
        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
        WorkflowTransitionUtil workflowTransitionUtil = (WorkflowTransitionUtil) JiraUtils.loadComponent(WorkflowTransitionUtilImpl.class);
        issueLinkManager.getOutwardLinks(issue.id)?.each { issueLink ->
            log.debug "Issue relations ship considered is: " + issueLink.issueLinkType.name
            if (issueLink.issueLinkType.name.equalsIgnoreCase(implementRelationship)) {
                log.debug "Linked issue is " + issueLink.getDestinationObject().getKey();
                MutableIssue destinationIssue = (MutableIssue) issueLink.getDestinationObject()

                boolean allInwardImplementLinksDone = true;
                for (def issueLinkNested : issueLinkManager.getInwardLinks(destinationIssue.id)) {
                    def sourceObject = issueLinkNested.getSourceObject();
                    def statusName = sourceObject.getStatus().getName();
                    if (sourceObject.getId() == issue.id) {
                        continue;
                    }
                    if (issueLinkNested.issueLinkType.name.equalsIgnoreCase(implementRelationship) && !finalStatuses.contains(statusName)) {
                        allInwardImplementLinksDone = false;
                        log.debug "Linked issue " + sourceObject.getKey() + " is not done."
                        break;
                    }
                }

                if (allInwardImplementLinksDone) {
                    workflowTransitionUtil.setIssue(destinationIssue);
                    log.debug "Issue is set. Assigning the ticket to: " + user.getKey();
                    workflowTransitionUtil.setUserkey(user.getKey());
                    log.debug "Assigned user is set"
                    workflowTransitionUtil.setAction(transitionId)
                    log.debug "Action is set"
                    workflowTransitionUtil.validate();
                    log.debug "Validationg action"
                    workflowTransitionUtil.progress();
                    log.debug "Action executed"
                }
            }
        }
    } catch (Exception e) {
        log.debug "Exception: " + e.getMessage();
    }
    log.debug "Returning from transitionTicketsImplementedByCurrentOne()";
}

def statusName = issue.getStatus().getName()
log.debug "Current ticket status name: " + statusName;
if (statusName.equalsIgnoreCase(doneStatus) || statusName.equalsIgnoreCase(closedStatus)) {
    log.debug "Will try to move implemented ticket to " + doneStatus;
    transitionTicketsImplementedByCurrentOne(doneActionId, finalStatuses, implementRelationship, log)
} else if (statusName.equalsIgnoreCase(inProgressStatus)) {
    log.debug "Will try to move implemented ticket to " + inProgressStatus;
    transitionTicketsImplementedByCurrentOne(progressActionId, finalStatuses, implementRelationship, log)
}