# CarboSystem
IP RC car system with google cardboard &amp; logitech G27

Carbo - Android App
- This app is used for transmission with PC Application(via socket), Headtracking Android Device(via socket) and Arduino(via UART).
- This app is using mik3y/usb-serial-for-android(https://github.com/mik3y/usb-serial-for-android) for UART transfer.
- This app use intent of IP Webcam(https://play.google.com/store/apps/details?id=com.pas.webcam) for streaming camera vision to HeadTrackingDemo App.
 
HeadTrackingDemo - Android App
- This app get sensor values with android device, transfer values to Carbo app via socket.
- It contains kalman filter 
- This app using webview for get image which streamed by Carbo app, but this repository do not contains webserver now.
 
MotorDrive - Arduino Firmware
- This firmware get user inputs from PC app(with G27) via Carbo app(with UART), and control servo / DC motor with PWM.
- Arduino(including this firmware) is replaced ESC module in RC Car
