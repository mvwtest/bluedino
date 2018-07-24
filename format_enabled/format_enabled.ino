#include <SoftwareSerial.h> // Include SoftwareSerial library
SoftwareSerial bt(3, 2);    // Create connection to bluetooth module  | PIN 3 as RX, PIN 2 as TX

// ** Variables **
char tmp;                   // Stores a character of the hole message | Checks if the message comes from BlueDuino, the mode and where the message ends
int pin;                    // Stores the value of the pin            | Used by analog write and digital write
int value;                  // Stores the analog value                | Used by anaog write
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

  /* If there's data it must be in one of the following formats
   *      Format      :     Mode
   *  [A.PIN.MESSAGE] : analog write
   *  [D.PIN.MESSAGE] : digital write
   *  [J.MESSAGE]     : joystick
   *  [T.MESSAGE]     : terminal
  */

  // Read the first character of the message, must be '['
  tmp = bt.read();
  if (tmp != '[') 
    return;
  else
    Serial.println("Format is not enabled, or message is not from BlueDuino");
  delay(5);

  // Read the second character of the message
  tmp = bt.read();
  
  // Check for each case, you can use if as well
  switch (tmp){
    case 'A': // Message from Analog write mode
      // Read the pin and value
      pin = bt.parseInt();
      value = bt.parseInt();
      // Your code
      Serial.println("Pin: " + String(pin) + " | Value: " + String(value)); // Optional, large messages might alter the readed values
      return;
    case 'D': // Message from Digital write mode
      // Read the pin and get the message
      pin = bt.parseInt();
      message = get_message();

      // Your code
      Serial.println("Pin: " + String(pin) + " | Value: " + message); // Optional, large messages might alter the readed values
      return;
    case 'J': // Message from Joystick mode
      message = get_message();
      
      // Your code
      Serial.println(message); // Optional, large messages might alter the readed values
      return;
    case 'T': // Message from Terminal mode
      message = get_message();
      
      // Your code
      Serial.println(message); // Optional, large messages might alter the readed values
      return;
  }
}

// Return the message without format
String get_message(){
  String text;
  char aux;
  // Read the first character to avoid adding '.' as the first character
  bt.read();
  delay(5);
  
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
