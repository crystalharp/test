老虎地图软件发布之前需要检查的细节：
1、各个服务（周边查询、公交查询、地图下载等等）的服务器URL应该是外网的URL；
2、客户端软件版本号；
3、检查提交到服务器的请求中的m值、pk值（sSPREADER、sPHONE_KEY）；
4、测试软件自动更新功能；
5、删除代码中无用的log打印信息；
6、版本号中的日期字段确定是发布当天的日期信息；
7、针对1.5和1.6平台发布安装包时需要去掉cdma定位和多点触摸功能；
8、删除test文件夹；
9、修改build.xml中的Line:326(将<arg value="-libraryjars ${android-jar}"/> 替换为相应平台android.jar的绝对路径)；
10、测试混淆版本的安装包；
11、将AndroidManifest.xml中android:debuggable="true"修改为“false“
