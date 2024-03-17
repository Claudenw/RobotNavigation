// ---------------------------------------------------------------------------
// Calculate a ping moving average using the ping_timer() method
// ---------------------------------------------------------------------------

#include <Wire.h>
#include <NewPing.h>

// Sonar code


#define SONAR_ITERATIONS 5     // Number of SONAR_ITERATIONS.
#define SONAR_TRIGGER_PIN 12    // Arduino pin tied to trigger pin on ping sensor.
#define SONAR_ECHO_PIN 11       // Arduino pin tied to echo pin on ping sensor.
#define SONAR_MAX_DISTANCE 500  // Maximum distance (in sonar_readings) to ping.
#define SONAR_PING_INTERVAL 50  // Milliseconds between sensor pings (29ms is about the min to avoid cross-sensor echo).
#define SONAR_NO_ECHO 501


unsigned int sonar_readings[SONAR_ITERATIONS];     // Where the ping distances are stored.
unsigned int current_iteration;                // Keeps track of iteration step.so
unsigned int sonar_time;
unsigned long next_trigger;
short bitcount[] = { 0, 1, 1, 2, 1, 2, 2, 3, 1, 2, 2, 3, 2, 3, 3, 4};

NewPing sonar(SONAR_TRIGGER_PIN, SONAR_ECHO_PIN, SONAR_MAX_DISTANCE);  // NewPing setup of pins and maximum distance.

void sonar_setup() {
  for (uint8_t i = 1; i < SONAR_ITERATIONS; i++) {  // Set the starting time for each iteration.
    sonar_readings[i] = SONAR_NO_ECHO;
  }
  current_iteration = 0;
  sonar_time = SONAR_NO_ECHO;
  next_trigger = 75 + SONAR_PING_INTERVAL + micros();
}

void sonar_cycle_complete() { // All iterations complete, calculate the median.
  unsigned int uS[SONAR_ITERATIONS];
  uint8_t j, it = SONAR_ITERATIONS;
  uS[0] = SONAR_NO_ECHO;
  //Serial.print("Readings: ");
  for (uint8_t i = 0; i < it; i++) { // Loop through iteration results.
      if (i > 0) {          // Don't start sort till second ping.
        for (j = i; j > 0 && uS[j - 1] < sonar_readings[i]; j--) // Insertion sort loop.
          uS[j] = uS[j - 1];                         // Shift ping array to correct position for sort insertion.
      } else j = 0;         // First ping is sort starting point.
      //Serial.print( sonar_readings[i] );
      //Serial.print( " " );
      uS[j] = sonar_readings[i];        // Add last ping to array in sorted position.
  }
  sonar_time = uS[it >> 1];
  //Serial.print("\Time: ");
  //Serial.println(sonar_time);
}

/* main loop */
void sonar_sense() {
  if (micros() >= next_trigger) {                            // Is it this iteration's time to ping?
    sonar_readings[current_iteration] = sonar.ping();
    current_iteration = (current_iteration + 1) % SONAR_ITERATIONS;
    if (current_iteration == 0)
      sonar_cycle_complete(); 
    next_trigger = SONAR_PING_INTERVAL + micros();                                            // Do the ping (processing continues, interrupt will call echoCheck to look for echo).
  }
}

uint8_t sonar_read() { 
  return sonar_time;
}

/// I2C code

byte i2c_rcv;  // data received from I2C bus

//received data handler function
void i2c_dataRcv(int numBytes) {
  while (Wire.available()) {  // read all bytes received
    //Serial.print("Read: ");
    //Serial.println( Wire.read(), HEX );
  }
}

// requests data handler function
void i2c_dataRqst() {
	unsigned int data = sonar_time;
  byte* datap = (byte*) &data;
  //Serial.print("Sonar: ");
  //Serial.println( data , HEX );
  //Serial.print( datap[0], HEX);
  //Serial.print( " ");
  //Serial.println(datap[1], HEX);
  int bits = bitcount[datap[0] & 0xF] + bitcount[(datap[0] >>4) & 0xF]+ bitcount[datap[1] & 0xF] + bitcount[(datap[1] >>4) & 0xF];
  //Serial.print( "bitcount: ");
  //Serial.println( bits );
  datap[1] |= (bits % 2) ? 0x80 : 0x0; 
  //Serial.print("Sending: ");
  //Serial.print( data , HEX );
  //Serial.print(" ");
  //Serial.println( data  );
  Wire.write( datap, 2);
}

void i2c_setup() {
  Wire.begin(0x08);  // join I2C bus as Slave with address 0x08

  // event handler initializations
  Wire.onReceive(i2c_dataRcv);   // register an event handler for received data
  Wire.onRequest(i2c_dataRqst);  // register an event handler for data requests

  // initialize global variables
  i2c_rcv = 255;
}
void setup() {

  i2c_setup();
  sonar_setup();
  //Serial.begin(115200);
}

void loop() {
  int last = 0;
  while (true) {
    sonar_sense();
  }
}
