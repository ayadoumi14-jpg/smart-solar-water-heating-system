#include <WiFi.h>
#include <Wire.h>
#include <LiquidCrystal_I2C.h>
#include <OneWire.h>
#include <DallasTemperature.h>
#include <ESP32Servo.h>

const char* ssid = "YOUR_WIFI_NAME";
const char* password = "YOUR_WIFI_PASSWORD";

IPAddress local_IP(192, 168, 1, 33);
IPAddress gateway(192, 168, 1, 1);
IPAddress subnet(255, 255, 255, 0);

WiFiServer server(80);
WiFiClient client;

#define ONE_WIRE_BUS 4

String data_rec;
OneWire oneWire(ONE_WIRE_BUS);
DallasTemperature sensors(&oneWire);
LiquidCrystal_I2C lcd(0x27,16,2);

Servo myservo_1, myservo_2;

int pos_1 = 85, pos_2 = 180;
int data_1, data_2, data_3, data_4;
int movee = 0, wanted = 0;

float volt, temp;

void setup() {

  Serial.begin(9600);

  if (!WiFi.config(local_IP, gateway, subnet)) {
    Serial.println("STA Failed to configure");
  }

  WiFi.begin(ssid, password);

  Serial.print("Connecting to WiFi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println("\nConnected!");
  Serial.println(WiFi.localIP());

  server.begin();

  pinMode(14, INPUT_PULLUP);
  pinMode(12, OUTPUT);
  pinMode(18, OUTPUT);
  pinMode(19, OUTPUT);
  pinMode(23, OUTPUT);
  analogSetAttenuation(ADC_11db);

  sensors.begin();
  Wire.begin(21,22);
  lcd.begin();
  lcd.backlight();

  myservo_1.attach(25);
  myservo_1.write(pos_1);

  myservo_2.attach(26);
  myservo_2.write(pos_2);
}

void loop() {

  WiFiClient newClient = server.available();
  if (newClient) {
    client = newClient;
    Serial.println("Client connected");
  }

  if (client && client.connected() && client.available()) {
    data_rec = client.readStringUntil('\n');
    wanted = data_rec.toInt();
    lcd.clear();
  }

  if ((digitalRead(14) == HIGH) && (digitalRead(12) == LOW)) {
    if (client && client.connected()) client.println("c");
    digitalWrite(12, HIGH);
    digitalWrite(18, HIGH);
    digitalWrite(23, HIGH);
  }

  if ((digitalRead(14) == LOW) && (digitalRead(23) == HIGH)) {
    digitalWrite(23, LOW);
  }

  int watterr = analogRead(39);

  if ((watterr > 500) && (digitalRead(12) == LOW)) {
    if (client && client.connected()) client.println("a");
    digitalWrite(18, HIGH);
    digitalWrite(12, HIGH);
  }

  if ((watterr < 500) && (digitalRead(12) == HIGH) && (digitalRead(14) == LOW)) {
    if (client && client.connected()) client.println("d");
    digitalWrite(12, LOW);
    digitalWrite(18, LOW);
  }

  int adcValue = analogRead(36);
  volt = (float)adcValue * 3.3 / 4095.0;
  volt = volt * 3.9;

  sensors.requestTemperatures();
  temp = sensors.getTempCByIndex(0);

  if (client && client.connected()) {
    client.println("V");
    client.println(volt);
    client.println("T");
    client.println(temp);
  }

  delay(200);

  if (temp >= wanted) {
    digitalWrite(19, LOW);
  } else {
    digitalWrite(19, HIGH);
  }

  lcd.setCursor(0,0);
  lcd.print("now");
  lcd.setCursor(0,1);
  lcd.print(temp);

  lcd.setCursor(6,0);
  lcd.print("want");
  lcd.setCursor(6,1);
  lcd.print(wanted);

  lcd.setCursor(11,0);
  lcd.print("volt");
  lcd.setCursor(11,1);
  lcd.print(volt);

  data_1 = analogRead(33);
  data_2 = analogRead(35);

  if (data_1 > 2800) myservo_1.write(70);
  if (data_2 > 2800) myservo_1.write(145);

  data_3 = analogRead(32);
  data_4 = analogRead(34);

  if ((data_3 > 2500) || (data_4 > 2500)) {
    if (movee == 0) {
      movee = 1;
      pos_2 = (pos_2 == 180) ? 80 : 180;
      myservo_2.write(pos_2);
    }
  } else {
    movee = 0;
  }
}
