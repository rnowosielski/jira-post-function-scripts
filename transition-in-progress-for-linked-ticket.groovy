import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.workflow.WorkflowTransitionUtil;
import com.atlassian.jira.workflow.WorkflowTransitionUtilImpl;
import com.atlassian.jira.util.JiraUtils;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.user.ApplicationUser

import org.apache.log4j.Category
def Category log = Category.getInstance("com.onresolve.jira.groovy.PostFunction")
log.setLevel(org.apache.log4j.Level.DEBUG)
log.debug "Will try to move linked tickets to In Progress"

def transitionTicketsImplementedByCurrentOne(int transitionId) {
    def issueLinkManager = ComponentAccessor.getIssueLinkManager();
    ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
    WorkflowTransitionUtil workflowTransitionUtil = ( WorkflowTransitionUtil ) JiraUtils.loadComponent( WorkflowTransitionUtilImpl.class );
    issueLinkManager.getOutwardLinks(issue.id)?.each { issueLink ->
        log.debug "Issue relations ship considered is: " + issueLink.issueLinkType.name
        if (issueLink.issueLinkType.name == "Implement") {
            log.debug "Linked issue is " + issueLink.getDestinationObject().getKey();
            MutableIssue issue = (MutableIssue) issueLink.getDestinationObject()
            workflowTransitionUtil.setIssue(issue);
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

def statusName = issue.getStatus().getName()
log.debug "Current ticket status name: " + statusName;
if (statusName == "Done" || statusName == "Closed") {
    transitionTicketsImplementedByCurrentOne(31)
} else if (statusName == "In Progress") {
    transitionTicketsImplementedByCurrentOne(21)
}