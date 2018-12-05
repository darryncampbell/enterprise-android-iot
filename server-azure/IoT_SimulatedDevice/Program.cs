// Based on https://github.com/Azure-Samples/azure-iot-samples-csharp/tree/master/iot-hub/Tutorials/Routing/SimulatedDevice, full
// credit to the original author

using Microsoft.Azure.Devices.Client;
using Newtonsoft.Json;
using System;
using System.Text;
using System.Threading.Tasks;

namespace SimulatedDevice
{
    class Program
    {
        private static DeviceClient s_deviceClient;
        private readonly static string s_myDeviceId = "test-device";    //  CHANGE THIS IF NECESSARY
        private readonly static string s_iotHubUri = "XXXXXXXXXXXXXX.azure-devices.net"; //  CHANGE THIS
        // This is the primary key for the device. This is in the portal. 
        // Find your IoT hub in the portal > IoT devices > select your device > copy the key. 
        private readonly static string s_deviceKey = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";  //  CHANGE THIS
        private static void Main(string[] args)
        {
            Console.WriteLine("Routing Tutorial: Simulated device\n");
            s_deviceClient = DeviceClient.Create(s_iotHubUri, new DeviceAuthenticationWithRegistrySymmetricKey(s_myDeviceId, s_deviceKey), TransportType.Mqtt);
            SendDeviceToCloudMessagesAsync();
            Console.WriteLine("Press the Enter key to stop.");
            Console.ReadLine();
        }
        private static async void SendDeviceToCloudMessagesAsync()
        {
            double minBattLevel = 10;
            double minBattHealth = 80;
            Random rand = new Random();

            while (true)
            {
                double currentBattLevel = minBattLevel + rand.NextDouble() * 25;
                double currentBattHealth = minBattHealth + rand.NextDouble() * 20;

                string levelValue;

                if (currentBattLevel < 15.0)
                {
                    levelValue = "critical";
                }
                else
                {
                    levelValue = "normal";
                }

                var telemetryDataPoint = new
                {
                    deviceId = s_myDeviceId,
                    dateTime = DateTime.Now.ToString(),
                    model = "TC57",
                    lat = "35.6602997",
                    lng = "139.7282743",
                    battLevel = currentBattLevel,
                    battHealth = currentBattHealth,
                    osVersion = "8.1.0",
                    patchLevel = "2019-02-01",
                    releaseVersion = "01-10-09.00-OG-U00-STD"
                };
                var telemetryDataString = JsonConvert.SerializeObject(telemetryDataPoint);

                //set the body of the message to the serialized value of the telemetry data
                var message = new Message(Encoding.ASCII.GetBytes(telemetryDataString));
                message.Properties.Add("batteryLevel", levelValue);

                await s_deviceClient.SendEventAsync(message);
                Console.WriteLine("{0} > Battery Level: {1} Sent message: {2}", DateTime.Now, levelValue, telemetryDataString);

                await Task.Delay(1000);
            }
        }
    }
}
