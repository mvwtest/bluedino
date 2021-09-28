#include <SoftwareSerial.h> // Include SoftwareSerial library
SoftwareSerial bt(3, 2);    // Create connection to bluetooth module  | PIN 3 as RX, PIN 2 as TX

// ** Variables **
String message;             // Stores the message without the format  | Used by digital, terminal and joystick

void setup() {
  Serial.begin(9600);
  bt.begin(9600);           // Serial communication with the buetooth module
}

void loop() {
  // Empty message variable
  message = "";

  // If there's no data return to te loop
  if (!bt.available()) return;

  /*  PREFIX DISABLED
   *      Format            :     Mode
   *  [PIN.MESSAGE]         : analog write
   *  [PIN.MESSAGE]         : digital write
   *  [MESSAGE]             : joystick
   *  [MESSAGE]             : terminal
   *
   *  PREFIX ENABLED
   *      Format            :     Mode
   *  [PREFIX.PIN.MESSAGE]  : analog write
   *  [PREFIX.PIN.MESSAGE]  : digital write
   *  [PREFIX.MESSAGE]      : joystick
   *  [PREFIX.MESSAGE]      : terminal
   *
   * YOU CAN REMOVE THE PIN SEPARATOR
   * FROM THE BLUEDINO SETTINGS
  */

  /* Large messages might cause issues, to fix it
   * change the send rate from the BlueDino App */
  message = get_message();
  Serial.println(message);
}

// Return the received message
String get_message(){
  String text;
  char aux;

  // While there's data
  while(bt.available() > 0){
    // Read the next character
    aux = bt.read();
    delay(5);
    // If it's the last character break the loop to avoid adding ']' as the last character
    if (aux == ']' && bt.available() == 0) break;
    // Add the character to the string
    text += aux;
  }

  // Return the message
  return text;
}
