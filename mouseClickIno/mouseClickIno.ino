#include <HID-Project.h>

void setup() {
  // put your setup code here, to run once:
  Mouse.begin();
}

void loop() {
  byte inByte;
  if(Serial.available())
  {
    inByte = Serial.read();
    Mouse.click();
    delay(150);
  }
}
