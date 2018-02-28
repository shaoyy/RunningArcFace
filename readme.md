RunningArcFace BetaV0.7
Created by shaoyy
通过调用虹软人脸识别接口实现人脸识别，为杭电阳光长跑定制
第一次运行初始化后，会在sd卡根目录下生成如下文件
/FaceArcData/img/
/FaceArcData/.reloaded
/FaceArcData/face.dat
其中，img需要存放用于识别的人脸与信息，文件命名格式为：学号 姓名.jpg
不可缺少后缀名、中间的空格，学号固定为8位

成功识别后会出现学号+姓名的按钮，点击完成识别
点击clear清空识别过的三个人（在识别三次都没有识别出自己的情况下需要点击）

通过重写MainActivity的submitMessage（String id,String name）方法，再具体实现与服务器的通信

参数设置介绍（写在MainActivity最后的静态常量）：
    public static final int CHECKTIME = 300;                设置检测人脸时间间隔
    public static final int MIN_FACE_SIZE = 16;             最小人脸（设置过小会把路人检测进来）
    public static final float MIN_RECOGNIZE = 0.4f;         最小匹配度（值越大，匹配越严格）
    public static final String DATAPATH = "/FaceArcData";   文件存储路径
    public static final boolean MOLT_PERSON = false;        是否重复记录检测到的人的信息（调成true会）
    public static final String VERSION = "BetaV0.7";        当前app版本

注1：权限或文件夹层次结构造成的app启动失败是存在的，重新启动即可正常运行
注2：要正常执行识别，请先将文件正确命名并放入img文件夹下，app会自动识别格式并读取。

目前支持的图片格式：JPEG
