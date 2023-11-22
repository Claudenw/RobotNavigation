// ---------------------------------------------------------------------------
// Calculate a ping moving average using the ping_timer() method
// ---------------------------------------------------------------------------

#include <Wire.h>
#include <NewPing.h>

// Sonar code

#define SONAR_ITERATIONS 5     // Number of SONAR_ITERATIONS.
#define SONAR_TRIGGER_PIN 12    // Arduino pin tied to trigger pin on ping sensor.
#define SONAR_ECHO_PIN 11       // Arduino pin tied to echo pin on ping sensor.
#define SONAR_MAX_DISTANCE 200  // Maximum distance (in sonar_readings) to ping.
#define SONAR_PING_INTERVAL 33  // Milliseconds between sensor pings (29ms is about the min to avoid cross-sensor echo).
#define SONAR_NO_ECHO 201

unsigned long sonar_ping_timer[SONAR_ITERATIONS];  // Holds the times when the next ping should happen for each iteration.
uint8_t sonar_readings[SONAR_ITERATIONS];     // Where the ping distances are stored.
unsigned int current_iteration;                // Keeps track of iteration step.so
uint8_t sonar_distance;

NewPing sonar(SONAR_TRIGGER_PIN, SONAR_ECHO_PIN, SONAR_MAX_DISTANCE);  // NewPing setup of pins and maximum distance.

void sonar_setup() {
  sonar_ping_timer[0] = millis() + 75;              // First ping starts at 75ms, gives time for the Arduino to chill before starting.
  for (uint8_t i = 1; i < SONAR_ITERATIONS; i++) {  // Set the starting time for each iteration.
    sonar_ping_timer[i] = sonar_ping_timer[i - 1] + SONAR_PING_INTERVAL;
    sonar_readings[i] = SONAR_NO_ECHO;
  }
  current_iteration = 0;
  sonar_distance = SONAR_NO_ECHO;
}

void sonar_cycle_complete() { // All iterations complete, calculate the median.
  uint8_t uS[SONAR_ITERATIONS];
  uint8_t j, it = SONAR_ITERATIONS;
  uS[0] = SONAR_NO_ECHO;
  for (uint8_t i = 0; i < it; i++) { // Loop through iteration results.
    if (sonar_readings[i] != SONAR_NO_ECHO) { // Ping in range, include as part of median.
      if (i > 0) {          // Don't start sort till second ping.
        for (j = i; j > 0 && uS[j - 1] < sonar_readings[i]; j--) // Insertion sort loop.
          uS[j] = uS[j - 1];                         // Shift ping array to correct position for sort insertion.
      } else j = 0;         // First ping is sort starting point.
      uS[j] = sonar_readings[i];        // Add last ping to array in sorted position.
    } else it--;            // Ping out of range, skip and don't include as part of median.
  }
  sonar_distance = uS[it >> 1];
}

void sonar_echo_check() {  // If ping received, set the sensor distance to array.
  if (sonar.check_timer()) {
    uint8_t result = sonar.ping_result / US_ROUNDTRIP_CM;
    sonar_readings[current_iteration] = result > 0 ? result : SONAR_NO_ECHO;
    current_iteration = (current_iteration + 1) % SONAR_ITERATIONS;
  }
}

/* main loop */
void sonar_sense() {
  if (millis() >= sonar_ping_timer[current_iteration]) {                            // Is it this iteration's time to ping?
    sonar_ping_timer[current_iteration] += SONAR_PING_INTERVAL * SONAR_ITERATIONS;  // Set next time this sensor will be pinged.
    sonar.timer_stop();                                                             // Make sure previous timer is canceled before starting a new ping (insurance).
    sonar.ping_timer(sonar_echo_check);                                             // Do the ping (processing continues, interrupt will call echoCheck to look for echo).
    if (current_iteration == 0)
      sonar_cycle_complete(); 
  }
}

uint8_t sonar_read() { 
  return sonar_distance;
}

/// I2C code

byte i2c_rcv;  // data received from I2C bus
byte i2c_cmd;

//received data handler function
void i2c_dataRcv(int numBytes) {
  if (Wire.available()) {
    i2c_cmd = Wire.read();
    while (Wire.available()) {  // read all bytes received
      Wire.read();
    }
  }
}

// requests data handler function
void i2c_dataRqst() {
  Serial.print("i2c Req: ");
  if (i2c_cmd == 1) {
    Wire.write(sonar_read());
  }
}

void i2c_setup() {
  Wire.begin(0x08);  // join I2C bus as Slave with address 0x08
  i2c_cmd = 1;

  // event handler initializations
  Wire.onReceive(i2c_dataRcv);   // register an event handler for received data
  Wire.onRequest(i2c_dataRqst);  // register an event handler for data requests

  // initialize global variables
  i2c_rcv = 255;
}
void setup() {
  Serial.begin(115200);  // Open serial monitor at 115200 baud to see ping results.
  i2c_setup();
  sonar_setup();
}

void loop() {
  int last = 0;
  while (true) {
    sonar_sense();
    int read = sonar_read();
    if (read != last) {
      last = read;
      Serial.print("sonar: ");
        Serial.println(read);
    }
  }
}
