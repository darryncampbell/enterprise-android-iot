/*
# Copyright Google Inc. 2017
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
*/
//const Datastore = require('@google-cloud/datastore');

// Instantiates a client
//const datastore = Datastore();

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
  /*
  const deviceProm = getDeviceBy(attrs[0]);
  deviceProm.then(devices => {
    const device = devices[0][0];
    controlDeviceTemperature(device, attrs[5]);
  });
	*/
  console.log(attrs[0] + ', ' + attrs[1] + ', ' + attrs[2] + ', ' + attrs[3] +
  ', ' + attrs[4] + ', ' + attrs[5] + ', ' + attrs[6] + ', ' + attrs[7] + ', ' + attrs[8] + ', ' + attrs[9]);
  
  var batteryLevel = attrs[5];
  var deviceId = attrs[0];
  if (batteryLevel <= 15) {
    console.error(new Error('Battery Level: ' + batteryLevel + ' less than error thredshold (15%) for ' + deviceId));
  }
  
  callback();
};
/*
function getDeviceBy (deviceName) {
  const query = datastore
    .createQuery('device')
    .filter('name', '=', deviceName);
  return datastore.runQuery(query);
}
*/
/*
function controlDeviceTemperature (device, batteryLevel) {
  if (batteryLevel <= 15) {
    console.error(new Error('Battery Level: ' + batteryLevel + ' less than error thredshold (15%) for ' + device.name));
  }
  else if (batteryLevel <= 25) {
    console.error(new Warning('Battery Level: ' + batteryLevel + ' less than warning thredshold (25%) for ' + device.name));
  }
}
*/