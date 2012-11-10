import sys
import time

# Imports the monkeyrunner modules used by this program
from com.android.monkeyrunner import MonkeyRunner, MonkeyDevice

startTime = 5
keyTime = 2
networkTime = 20

sdcard = '/sdcard/'
apk = ' '
removepackage = False
deletemap = False
island = False
width = 480
height = 800
titlebarheight = 80

# sets a variable with the package's internal name
package = 'com.tigerknows'

# sets a variable with the name of an Activity in the package
activity = 'com.tigerknows.Sphinx'

def takeSnapshot(filename):
    # Takes a screenshot
    result = device.takeSnapshot()

    # Writes the screenshot to a file
    result.writeToFile(filename, 'png')
    # pass

def pressKeyTimes(keycode, times):
    for i in range(0, times):
        device.press(keycode, 'DOWN_AND_UP')
        time.sleep(keyTime);

def pressKeyList(keycodes):
    for keycode in keycodes.split(','):
        device.press(keycode, 'DOWN_AND_UP')
        time.sleep(keyTime);

def selectMenu(index):
    pressKeyTimes('KEYCODE_MENU', 1)
    pressKeyTimes('KEYCODE_DPAD_LEFT', 1)
    if island == False and index > 3:
        pressKeyTimes('KEYCODE_DPAD_DOWN', index)
        pressKeyTimes('KEYCODE_DPAD_RIGHT', (index - 1) % 3)
    else:
        pressKeyTimes('KEYCODE_DPAD_RIGHT', index - 1)
    pressKeyTimes('KEYCODE_ENTER', 1)

# Menu UI
def menu():
    pressKeyTimes('KEYCODE_MENU', 1)
    takeSnapshot('press_menu_map.png')
    pressKeyTimes('KEYCODE_BACK', 1)

# Search POI UI
def searchPoi():
    selectMenu(1)
    takeSnapshot('searchpoi.png')
    pressKeyList('KEYCODE_DPAD_UP,KEYCODE_DPAD_LEFT,KEYCODE_ENTER')
    takeSnapshot('searchpoi-selectarea.png')
    pressKeyTimes('KEYCODE_BACK', 1)
    device.touch(width/2, titlebarheight + 25, 'DOWN_AND_UP')
    pressKeyList('KEYCODE_W,KEYCODE_F,KEYCODE_J,KEYCODE_DPAD_RIGHT,KEYCODE_ENTER')
    time.sleep(networkTime);
    takeSnapshot('searchpoi-result.png')
    pressKeyList('KEYCODE_DPAD_LEFT,KEYCODE_DPAD_RIGHT,KEYCODE_DPAD_DOWN,KEYCODE_ENTER')
    takeSnapshot('searchpoi-poidetail.png')
    pressKeyTimes('KEYCODE_BACK', 3)

# Discover UI
def discover():
    selectMenu(2)
    takeSnapshot('discover-waiting.png')
    time.sleep(networkTime);
    takeSnapshot('discover.png')
    pressKeyTimes('KEYCODE_BACK', 1)

# Query Traffic UI
def queryTraffic():
    selectMenu(3)
    takeSnapshot('querytraffic.png')
    pressKeyTimes('KEYCODE_DPAD_UP', 5)
    pressKeyList('KEYCODE_DPAD_LEFT,KEYCODE_DPAD_DOWN,KEYCODE_DPAD_RIGHT,KEYCODE_ENTER')
    takeSnapshot('querytraffic-selectstart.png')
    pressKeyList('KEYCODE_DPAD_UP,KEYCODE_DPAD_LEFT,KEYCODE_ENTER')
    selectMenu(1)
    
    # A select point
    #pressKeyList('KEYCODE_DPAD_DOWN,KEYCODE_DPAD_RIGHT,KEYCODE_ENTER,KEYCODE_DPAD_LEFT,KEYCODE_ENTER')
    #pressKeyTimes('KEYCODE_DPAD_LEFT', 5)
    #device.touch(width/2, height/2, 'DOWN_AND_UP')
    #selectMenu(1)
    
    # B input location name
    pressKeyList('KEYCODE_DPAD_DOWN')
    pressKeyList('KEYCODE_W,KEYCODE_F,KEYCODE_J')
    pressKeyList('KEYCODE_DPAD_RIGHT,KEYCODE_DPAD_DOWN,KEYCODE_ENTER')
    time.sleep(networkTime);
    pressKeyList('KEYCODE_DPAD_UP,KEYCODE_DPAD_LEFT,KEYCODE_ENTER')
    pressKeyTimes('KEYCODE_DPAD_UP', 6)
    pressKeyList('KEYCODE_DPAD_RIGHT,KEYCODE_ENTER')
    time.sleep(networkTime);
    pressKeyList('KEYCODE_DPAD_UP,KEYCODE_DPAD_LEFT,KEYCODE_ENTER')
    
    time.sleep(networkTime);
    pressKeyList('KEYCODE_DPAD_DOWN,KEYCODE_ENTER,KEYCODE_BACK')

    # B input location name
    pressKeyTimes('KEYCODE_BACK', 1)

    # Query Busline
    pressKeyTimes('KEYCODE_DPAD_UP', 5)
    pressKeyList('KEYCODE_DPAD_LEFT,KEYCODE_DPAD_RIGHT,KEYCODE_DPAD_DOWN,KEYCODE_DPAD_LEFT,KEYCODE_1,KEYCODE_DPAD_RIGHT,KEYCODE_ENTER')
    time.sleep(networkTime);
    pressKeyTimes('KEYCODE_ENTER', 1)
    pressKeyTimes('KEYCODE_BACK', 2)

# Favorite UI
def favorite():
    selectMenu(4)
    takeSnapshot('favorite.png')
    pressKeyTimes('KEYCODE_BACK', 1)

# More UI
def more():
    selectMenu(6)
    takeSnapshot('more.png')

    pressKeyList('KEYCODE_DPAD_LEFT,KEYCODE_ENTER')
    takeSnapshot('more-settings.png')
    pressKeyList('KEYCODE_BACK,KEYCODE_DPAD_LEFT,KEYCODE_DPAD_DOWN,KEYCODE_ENTER')
    takeSnapshot('more-selectcity.png')
    pressKeyList('KEYCODE_DPAD_LEFT,KEYCODE_DPAD_UP,KEYCODE_ENTER')
    selectMenu(6)
    pressKeyList('KEYCODE_DPAD_LEFT,KEYCODE_DPAD_DOWN,KEYCODE_ENTER')
    takeSnapshot('more-mapdownload-waiting.png')
    time.sleep(networkTime);
    takeSnapshot('more-mapdownload.png')
    pressKeyList('KEYCODE_BACK,KEYCODE_DPAD_LEFT,KEYCODE_DPAD_DOWN')

    # Send map
    # pressKeyTimes('KEYCODE_ENTER', 1)
    # takeSnapshot('more-sendmap.png')
    # pressKeyTimes('KEYCODE_BACK', 1)

    pressKeyList('KEYCODE_DPAD_LEFT,KEYCODE_DPAD_DOWN,KEYCODE_ENTER')
    takeSnapshot('more-history.png')
    pressKeyTimes('KEYCODE_MENU', 1)
    takeSnapshot('more-history-menu.png')
    pressKeyList('KEYCODE_BACK,KEYCODE_DPAD_LEFT,KEYCODE_DPAD_DOWN,KEYCODE_ENTER,KEYCODE_MENU')
    takeSnapshot('more-history-in-menu.png')
    pressKeyTimes('KEYCODE_BACK', 3)

    pressKeyList('KEYCODE_DPAD_LEFT,KEYCODE_DPAD_DOWN,KEYCODE_ENTER')
    takeSnapshot('more-checkupdate.png')
    time.sleep(networkTime);

    pressKeyList('KEYCODE_DPAD_LEFT,KEYCODE_DPAD_DOWN,KEYCODE_ENTER')
    takeSnapshot('more-feedback.png')
    pressKeyList('KEYCODE_BACK,KEYCODE_DPAD_LEFT,KEYCODE_DPAD_DOWN,KEYCODE_ENTER')
    takeSnapshot('more-help.png')
    pressKeyList('KEYCODE_BACK,KEYCODE_DPAD_LEFT,KEYCODE_DPAD_DOWN,KEYCODE_ENTER')
    takeSnapshot('more-about.png')
    pressKeyTimes('KEYCODE_BACK', 2)


if len(sys.argv) >= 2:
    try:
        argvs = sys.argv[1].split(',')
        i = 0
        for argv in argvs:
            if argv == 'apk':
                i = i + 1
                apk = argvs[i]
            elif argv == 'package':
                i = i + 1
                package = argvs[i]
            elif argv == 'activity':
                i = i + 1
                activity = argvs[i]
            elif argv == 'sdcard':
                i = i + 1
                sdcard = argvs[i]
            elif argv == 'starttime':
                i = i + 1
                startTime = argvs[i]
            elif argv == 'keytime':
                i = i + 1
                keyTime = argvs[i]
            elif argv == 'networktime':
                i = i + 1
                networkTime = argvs[i]
            elif argv == 'deletemap':
                i = i + 1
                deletemap = argvs[i]
            elif argv == 'removepackage':
                i = i + 1
                removepackage = argvs[i]
            elif argv == 'island':
                i = i + 1
                island = argvs[i]
            elif argv == 'width':
                i = i + 1
                width = argvs[i]
            elif argv == 'height':
                i = i + 1
                height = argvs[i]
            elif argv == 'titlebarheight':
                i = i + 1
                titlebarheight = argvs[i]
            i = i + 1
    except:
        print '\argv error/exception occurred.'


# Connects to the current returning a MonkeyDevice object
device = MonkeyRunner.waitForConnection()

if removepackage == True:
    device.removePackage(package)

if deletemap == True:
    device.shell('rm -r ' + sdcard + 'tigermap')

# Installs the Android package. Notice that this method returns a boolean, so you can test
# to see if the installation worked.
if apk != ' ':
    device.installPackage(apk)

# sets the name of the component to start
runComponent = package + '/' + activity

# Runs the component
device.startActivity(component=runComponent)
time.sleep(startTime);
takeSnapshot('map.png')

menu()
more()
searchPoi()
queryTraffic()
discover()
favorite()

# Finish
pressKeyList('KEYCODE_BACK,KEYCODE_ENTER')