using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.SignalR;
using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Extensions.Http;
using Microsoft.Azure.WebJobs.Extensions.SignalRService;
using Microsoft.Extensions.Logging;

// SignalR negotiation function
public class negotiate
{
    [FunctionName("negotiate")]
    public static SignalRConnectionInfo Run(
        [HttpTrigger(AuthorizationLevel.Anonymous, "get", "post")] HttpRequest req,
        [SignalRConnectionInfo(HubName = "mapcommshub")] SignalRConnectionInfo connectionInfo,
        ILogger log
    )
    {
        log.LogInformation("Negotiate function processed a request.");

        return connectionInfo;
    }
}

// SignalR hub class
public class mapcommshub : Hub
{
    // Dictionary to store group memberships
    private static Dictionary<string, HashSet<string>> groupUsers = new Dictionary<
        string,
        HashSet<string>
    >();

    // Function to handle joining a group
    [FunctionName("JoinGroup")]
    public static async Task Run(
        [SignalRTrigger(
            "mapcommshub",
            "messages",
            "JoinGroup",
            ConnectionStringSetting = "AzureSignalRConnectionString"
        )]
            InvocationContext invocationContext,
        [SignalR(HubName = "mapcommshub")] IAsyncCollector<SignalRGroupAction> signalRGroupActions,
        ILogger log
    )
    {
        try
        {
            // Extract parameters from the invocation context
            string groupName = invocationContext.Arguments[0].ToString();
            string username = invocationContext.Arguments[1].ToString();

            log.LogInformation($"Received group name: {groupName}, username: {username}");

            // Ensure the group exists in the dictionary
            if (!groupUsers.ContainsKey(groupName))
            {
                groupUsers[groupName] = new HashSet<string>();
            }

            // Check if the user is already in the group
            if (groupUsers[groupName].Contains(username))
            {
                log.LogInformation(
                    $"User with existing username '{username}' is already in the group. Joining not allowed."
                );
            }
            else
            {
                // Add the user to the group
                groupUsers[groupName].Add(username);

                // Create a SignalRGroupAction to notify clients about the user joining the group
                var groupAction = new SignalRGroupAction
                {
                    ConnectionId = invocationContext.ConnectionId,
                    GroupName = groupName,
                    Action = GroupAction.Add
                };

                // Add the group action to the SignalR group
                await signalRGroupActions.AddAsync(groupAction);

                log.LogInformation("Group action added successfully.");
            }
        }
        catch (Exception ex)
        {
            log.LogError($"Error processing the request: {ex.Message}");
        }
    }

    // Function to handle leaving a group
    [FunctionName("LeaveGroup")]
    public static async Task LeaveGroup(
        [SignalRTrigger("mapcommshub", "messages", "LeaveGroup")]
            InvocationContext invocationContext,
        [SignalR(HubName = "mapcommshub")] IAsyncCollector<SignalRGroupAction> signalRGroupActions,
        ILogger log
    )
    {
        try
        {
            // Extract parameters from the invocation context
            string connectionId = invocationContext.ConnectionId;
            string groupName = invocationContext.Arguments[0].ToString();

            // Create a SignalRGroupAction to notify clients about the user leaving the group
            await signalRGroupActions.AddAsync(
                new SignalRGroupAction
                {
                    ConnectionId = connectionId,
                    GroupName = groupName,
                    Action = GroupAction.Remove
                }
            );

            log.LogInformation($"Connection {connectionId} left the group: {groupName}");
        }
        catch (Exception ex)
        {
            log.LogError($"Error processing the request: {ex.Message}");
            // You might want to handle errors specific to SignalRTrigger here
        }
    }

    // Function to handle sending map data to a group
    [FunctionName("SendMapData")]
    public static async Task SendMapData(
        [SignalRTrigger(
            "mapcommshub",
            "messages",
            "SendMapData",
            ConnectionStringSetting = "AzureSignalRConnectionString"
        )]
            InvocationContext invocationContext,
        [SignalR(HubName = "mapcommshub")] IAsyncCollector<SignalRMessage> signalRMessages,
        ILogger log
    )
    {
        try
        {
            // Extract parameters from the invocation context
            string groupName = invocationContext.Arguments[0]?.ToString();

            // Check if the group name is present
            if (groupName != null)
            {
                // Dynamically retrieve the message content from arguments
                string messageContent =
                    invocationContext.Arguments.Length > 1
                        ? invocationContext.Arguments[1]?.ToString()
                        : null;

                // Send the log message to all connected clients in the group
                await signalRMessages.AddAsync(
                    new SignalRMessage
                    {
                        Target = "receiveLogs", // Specify the target method on the client side
                        Arguments = new[] { messageContent },
                        GroupName = groupName // Specify the group name
                    }
                );

                log.LogInformation(
                    $"Connection sent message: {messageContent} to all connected clients in the group {groupName}"
                );
            }
            else
            {
                log.LogError("Group name is missing. Unable to send message.");
            }
        }
        catch (Exception ex)
        {
            log.LogError($"Error processing the request: {ex.Message}");
            // You might want to handle errors specific to SignalRTrigger here
        }
    }
}
