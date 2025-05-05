// ---------------------------------------------------------------------------
// Calculate a ping moving average using the ping_timer() method
// ---------------------------------------------------------------------------


#include <IRDetector.h>
#include <XMotor.h>
#include <XSonar.h>

#define SONAR_ITERATIONS 5      // Number of SONAR_ITERATIONS.
#define SONAR_TRIGGER_PIN 10     // Arduino pin tied to trigger pin on ping sensor.
#define SONAR_ECHO_PIN 11       // Arduino pin tied to echo pin on ping sensor.
#define SONAR_PING_INTERVAL 50  // Milliseconds between sensor pings (29ms is about the min to avoid cross-sensor echo).

#define REVERSE 10
#define FULL_STOP 20



IRDetector ir_left = IRDetector(A0);
IRDetector ir_right = IRDetector(A3);
XMotor motor_left = XMotor(4, 5);
XMotor motor_right = XMotor(2, 3);
XSonar sonar = XSonar(SONAR_ITERATIONS, SONAR_PING_INTERVAL, SONAR_TRIGGER_PIN, SONAR_ECHO_PIN);

uint8_t sleepPin = 9;
uint8_t faultPin = 8;

void setup() {
  pinMode(sleepPin, OUTPUT);
  pinMode(faultPin, INPUT);
  
  Serial.begin(9600);
  digitalWrite(sleepPin, HIGH);
}

void loop() {
  sonar.sense();
  int dist = sonar.read();
  bool irL = ir_left.detect();
  bool irR = ir_right.detect();

  if (digitalRead(faultPin) == HIGH) {
    Serial.print(" >>> FAULT <<< ");
  }
  Serial.print("d: ");
  Serial.print(dist);
  Serial.print(" l:");
  Serial.print(irL);
  Serial.print(" r:");
  Serial.print(irR);


  if (dist == 0) {
    if (irL || irR) {
      motor_left.reverse();
      motor_right.reverse();
      Serial.println(" - full reverse");
    } else {
      motor_left.stop();
      motor_right.stop();
      Serial.println(" - full stop");
    }
  } else if (dist < REVERSE) {
    motor_left.reverse();
    motor_right.reverse();
    Serial.println(" - full reverse");
  } else if (dist < FULL_STOP) {
    motor_left.stop();
    motor_right.stop();
    Serial.println(" - full stop");
  } else {
    if (irL && irR) {
      motor_left.reverse();
      motor_right.reverse();
      Serial.println(" - full reverse");
    } else {
      if (irL) {
        motor_left.stop();
        motor_right.reverse();
        Serial.println(" - right turn");

      } else if (irR) {
        motor_left.reverse();
        motor_right.stop();
        Serial.println(" - left turn");
      } else {
        motor_left.forward();
        motor_right.forward();
        Serial.println(" - forward");
      }
    }
  }
}
