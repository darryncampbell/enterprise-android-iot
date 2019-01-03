//  Based heavily on original file located at https://github.com/GoogleCloudPlatform/community/tree/master/tutorials/cloud-iot-rtdp/function.  Full credit to original author

exports.iot = function (event, callback) {
  const pubsubMessage = event.data;
  var attrs = Buffer.from(pubsubMessage.data, 'base64').toString().split(',');

  //  Device ID			==> attrs[0]
  //  Date Time			==> attrs[1]
  //  Model 			==> attrs[2]
  //  Latitude			==> attrs[3]
  //  Longitude			==> attrs[4]
  //  Battery Level		==> attrs[5]
  //  Battery Health	==> attrs[6]
  //  OS Version		==> attrs[7]
  //  Patch Level		==> attrs[8]
  //  Release Version	==> attrs[9]
  console.log(attrs[0] + ', ' + attrs[1] + ', ' + attrs[2] + ', ' + attrs[3] +
  ', ' + attrs[4] + ', ' + attrs[5] + ', ' + attrs[6] + ', ' + attrs[7] + ', ' + attrs[8] + ', ' + attrs[9]);
  
  var batteryLevel = attrs[5];
  var deviceId = attrs[0];
  if (batteryLevel <= 15) {
    console.error(new Error('Battery Level: ' + batteryLevel + ' less than error threshold (15%) for ' + deviceId));
  }
  
  callback();
};
