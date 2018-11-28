console.log('Loading function');
var AWS = require('aws-sdk');
var dynamo = new AWS.DynamoDB.DocumentClient();
var table = "device-data";

exports.handler = function(event, context) {
   var params = {
    TableName:table,
    Item:{
        "deviceId": event.deviceId,
        "dateTime": event.dateTime,
        "model": event.model,
        "lat": event.lat,
        "lng": event.lng,
        "battLevel": event.battLevel,
        "battHealth": event.battHealth,
        "osVersion": event.osVersion,
        "patchLevel": event.patchLevel,
        "releaseVersion": event.releaseVersion
        }
    };

    console.log("Received device data");
    if (event.battLevel < "16")
        console.log("BATTERY LOW for device " + event.deviceId + " (" + event.model + "), " + event.battLevel + "%");
    dynamo.put(params, function(err, data) {
        if (err) {
            console.error("Unable to add device. Error JSON:", JSON.stringify(err, null, 2));
            context.fail();
        } else {
            console.log("Received device data:", JSON.stringify(params.Item));
            context.succeed();
        }
    });
}