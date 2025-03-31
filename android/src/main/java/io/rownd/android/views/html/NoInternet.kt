package io.rownd.android.views.html

import android.content.Context
import io.rownd.android.Rownd
import io.rownd.android.util.convertRGBtoString

fun noInternetHTML(context: Context): String {
    val appConfig = Rownd.store.currentState.appConfig
    val fontSize = Rownd.config.customizations.defaultFontSize
    val primaryColor = appConfig?.config?.customizations?.primaryColor ?: "#5b13df"

    // Set Background Color
    val dynamicSheetBackgroundColor = Rownd.config.customizations.dynamicSheetBackgroundColor
    val sheetBackgroundColor = Rownd.config.customizations.sheetBackgroundColor
    var backgroundColor = convertRGBtoString(dynamicSheetBackgroundColor.red, dynamicSheetBackgroundColor.green, dynamicSheetBackgroundColor.blue)
    if (sheetBackgroundColor != null) {
        backgroundColor = convertRGBtoString(sheetBackgroundColor.red, sheetBackgroundColor.green, sheetBackgroundColor.blue)
    }

    // Set Dark Mode
    var isDarkMode = Rownd.config.customizations.isNightMode()
    val darkMode = appConfig?.config?.hub?.customizations?.darkMode
    if (darkMode != null) {
        isDarkMode = darkMode == "enabled" || (isDarkMode && darkMode == "auto")
    }

    return """
        <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1, user-scalable=no">
                <style>
                    ${noInternetCSS(fontSize, backgroundColor, primaryColor, isDarkMode)}
                </style>
                <script>
                    function tryAgain() {
                        if (window?.rowndAndroidSDK) {
                            const buttonElem = document.querySelector('button');
                            buttonElem.classList.add('loading');
                            setTimeout(()=> {
                                window.rowndAndroidSDK.postMessage('{"type":"try_again"}');
                            }, 1000);
                            setTimeout(()=> {
                                buttonElem.classList.remove('loading');
                            }, 4000);
                        }
                    }
                </script>
            </head>
            <body>
                <h1>You're offline</h1>
                <p>Please check your connection and try again</p>
                <div class="wifi ${if (isDarkMode) "wifi-dark" else ""}"></div>
                <button onclick="tryAgain()">Try again<span></span></button>
            </body>
        </html>
    """.trimIndent()
}

fun noInternetCSS(fontSize: Float, backgroundColor: String?, primaryColor: String, isDarkMode: Boolean): String {
    return """
    body {
            font-size: ${fontSize}px;
            color: ${if (isDarkMode) "white" else "black"};
            display: flex;
            flex-direction: column;
            align-items: center;
            width: 100%;
            margin: 0px;
            padding: 0px;
            font-family: Arial, sans-serif;
            background-color: ${backgroundColor ?: "transparent"};
        }
        h1 {
            margin-top: 40px;
            font-size: 1.5em;
            font-weight: 400;
        }
        p {
            font-size: 1.166em;
            margin-top: 0px;
            margin-bottom: 20px;
        }
        img {
            height: 80px;
            width: 80px;
        }
        .wifi {
            height: 80px;
            width: 80px;
            background-position: center;
            background-repeat: no-repeat;
            background-image: url("data:image/svg+xml,%3C%3Fxml version='1.0' encoding='UTF-8'%3F%3E%3Csvg width='72px' height='72px' viewBox='0 0 72 72' version='1.1' xmlns='http://www.w3.org/2000/svg' xmlns:xlink='http://www.w3.org/1999/xlink'%3E%3Ctitle%3EGroup%3C/title%3E%3Cg id='Stardust-PTA' stroke='none' stroke-width='1' fill='none' fill-rule='evenodd'%3E%3Cg id='sm-320px-4-column-copy-45' transform='translate(-124.000000, -469.000000)'%3E%3Cg id='Group' transform='translate(124.000000, 469.000000)'%3E%3Ccircle id='Oval' fill='%235B13DF' opacity='0.1' cx='36' cy='36' r='36'%3E%3C/circle%3E%3Cg id='Enterprise-/-Commerce-/-account-/-24' transform='translate(18.500000, 18.500000)' fill='%23151515'%3E%3Cpath d='M17.5,31.25 C18.8807119,31.25 20,30.1307119 20,28.75 C20,27.3692881 18.8807119,26.25 17.5,26.25 C16.1192881,26.25 15,27.3692881 15,28.75 C15,30.1307119 16.1192881,31.25 17.5,31.25 Z M35,1.767625 L33.232375,0 L0,33.232375 L1.767625,35 L15.12025,21.647375 C17.7361152,20.7549793 20.6311822,21.390497 22.63275,23.2965 L24.4,21.5295 C22.6664335,19.8690364 20.3914316,18.8906302 17.99375,18.774375 L22.228125,14.53975 C24.3655373,15.2484624 26.315792,16.4296724 27.934,17.995625 L29.7,16.228625 C28.1038198,14.6776982 26.2259778,13.4460698 24.1675,12.6 L27.9135,8.85375 C29.8651406,9.86712934 31.6570764,11.1621195 33.23175,12.697125 L35,10.929 L35,10.9265 C33.4335399,9.39557885 31.6728707,8.07693652 29.763125,7.004375 L35,1.767625 Z M15.85,13.847 L18.401875,11.295125 C18.10125,11.27925 17.804625,11.2499996 17.5,11.2499996 C12.932068,11.2489927 8.54627415,13.041149 5.28625,16.240875 L7.05325,18.007875 C9.43997344,15.6794995 12.5360421,14.2150547 15.85,13.847 L15.85,13.847 Z M17.5,6.25 C19.2833941,6.25693715 21.0595403,6.47742369 22.7905,6.90675 L24.84375,4.8535 C16.0758236,2.15498655 6.53425729,4.48740142 0,10.9265 L0,10.954625 L1.755375,12.71 C5.94931349,8.56914478 11.6062916,6.2480937 17.5,6.25 L17.5,6.25 Z' id='Fill'%3E%3C/path%3E%3C/g%3E%3C/g%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
          }
        .wifi-dark {
            background-image: url("data:image/svg+xml,%3C%3Fxml version='1.0' encoding='UTF-8'%3F%3E%3Csvg width='72px' height='72px' viewBox='0 0 72 72' version='1.1' xmlns='http://www.w3.org/2000/svg' xmlns:xlink='http://www.w3.org/1999/xlink'%3E%3Ctitle%3EGroup 2%3C/title%3E%3Cg id='Stardust-PTA' stroke='none' stroke-width='1' fill='none' fill-rule='evenodd'%3E%3Cg id='Stardust--Copy-87' transform='translate(-124.000000, -481.000000)'%3E%3Cg id='Group' transform='translate(24.000000, 411.000000)'%3E%3Cg id='Group-2' transform='translate(100.000000, 70.000000)'%3E%3Ccircle id='Oval' fill-opacity='0.2' fill='%23000000' cx='36' cy='36' r='36'%3E%3C/circle%3E%3Cg id='Enterprise-/-Commerce-/-account-/-24' transform='translate(18.500000, 18.500000)' fill='%23E0E0E0'%3E%3Cpath d='M17.5,31.25 C18.8807119,31.25 20,30.1307119 20,28.75 C20,27.3692881 18.8807119,26.25 17.5,26.25 C16.1192881,26.25 15,27.3692881 15,28.75 C15,30.1307119 16.1192881,31.25 17.5,31.25 Z M35,1.767625 L33.232375,0 L0,33.232375 L1.767625,35 L15.12025,21.647375 C17.7361152,20.7549793 20.6311822,21.390497 22.63275,23.2965 L24.4,21.5295 C22.6664335,19.8690364 20.3914316,18.8906302 17.99375,18.774375 L22.228125,14.53975 C24.3655373,15.2484624 26.315792,16.4296724 27.934,17.995625 L29.7,16.228625 C28.1038198,14.6776982 26.2259778,13.4460698 24.1675,12.6 L27.9135,8.85375 C29.8651406,9.86712934 31.6570764,11.1621195 33.23175,12.697125 L35,10.929 L35,10.9265 C33.4335399,9.39557885 31.6728707,8.07693652 29.763125,7.004375 L35,1.767625 Z M15.85,13.847 L18.401875,11.295125 C18.10125,11.27925 17.804625,11.2499996 17.5,11.2499996 C12.932068,11.2489927 8.54627415,13.041149 5.28625,16.240875 L7.05325,18.007875 C9.43997344,15.6794995 12.5360421,14.2150547 15.85,13.847 L15.85,13.847 Z M17.5,6.25 C19.2833941,6.25693715 21.0595403,6.47742369 22.7905,6.90675 L24.84375,4.8535 C16.0758236,2.15498655 6.53425729,4.48740142 0,10.9265 L0,10.954625 L1.755375,12.71 C5.94931349,8.56914478 11.6062916,6.2480937 17.5,6.25 L17.5,6.25 Z' id='Fill'%3E%3C/path%3E%3C/g%3E%3C/g%3E%3C/g%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
        }
        button {
            color: white;
            background-color: $primaryColor;
            margin-top: 20px;
            font-size: 1.166em;
            width: 80%;
            padding: 10px 0px;
            border-radius: 12px;
            outline: none;
            border: none;
            position: relative;
        }
        @keyframes button-loading-spinner {
            from {
              transform: rotate(0turn);
            }
            to {
              transform: rotate(1turn);
            }
        }
        .loading {
            color: transparent;
        }
        .loading::after {
            content: '';
            position: absolute;
            width: 16px;
            height: 16px;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            margin: auto;
            border: 4px solid transparent;
            border-top-color: white;
            border-radius: 50%;
            animation: button-loading-spinner 1s ease infinite;
        }   
    """.trimIndent()
}