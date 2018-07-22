#include <SoftwareSerial.h> // Include SoftwareSerial library
                            // Inlcuir libreria SoftwareSerial
SoftwareSerial bt(5, 4);    // Create connection to bluetooth module | PIN 5 as RX, PIN 4 as TX
                            // Crea conexión al módulo bluetooth | PIN 5 como RX, PIN 4 como TX

String command;             // Variable que acumula el mensaje tipo String
char check;                 // Variable que verifica si el mensaje es enviado desde BlueDuino
int id;                     // Variable que recibe el el modo [Analog = 'A', Digital = 'D', Joystick = 'J', Terminal = 'T']
int pin;                    // Variable que recibe el pin
int value;                  // Variable que recibe el valor para asignar al pin

void setup() {
  Serial.begin(9600);
  bt.begin(9600);           // Iniciar comunicación serial con el módulo bluetooth
}

void loop() {
  // If there's no data return
  if (bt.available() < 1) return;

  // If 
  check = bt.read();
  if (check != '[') return;

  delay(5);
  check = bt.read();

  String message;
  char tmp;
  
  switch(check){
    case 'A':
      pin = bt.parseInt();
      value = bt.parseInt();
      Serial.println("Pin: " + String(pin) + " | Value: " + String(value));
      return;
    case 'D':
      pin = bt.parseInt();
      delay(5);
      bt.read();
      while(bt.available() > 0){
        delay(5);
        tmp = bt.read();
        if (tmp == ']' && (bt.available() == 0))
          break;
        message += tmp;
      }
      Serial.println("Message: " + message);
      return;
    case 'T':
      delay(5);
      bt.read();
      while(bt.available() > 0){
        delay(5);
        tmp = bt.read();
        if (tmp == ']' && (bt.available() == 0))
          break;
        message += tmp;
      }
      Serial.println("Message: " + message);
      return;
  }
}

