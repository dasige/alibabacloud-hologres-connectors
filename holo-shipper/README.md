# holo-shipper

## 功能介绍
holo-shipper 是支持将Holo Instance的部分表导入导出的备份工具。可以在Holo之间搬迁表，也可以dump到中间存储然后再恢复。

## 要求
- 具备superuser权限的账号
- 能连接上源和目标并且有JAVA环境的机器

### 命令行参数
-s ship的源头，可以为Holo instance, OSS或本地存储， required 

    Holo的格式： -s holo -h ip_number -p port_number -u username -w password

    OSS的格式: -s oss -h endpoint -u accessKeyId -w accessKeySecret -b bucketName -p folder_path

    本地存储的格式：-s local_path

-d ship的终点，可以为Holo instance， OSS或本地存储， required

    Holo的格式： -d holo -h ip_number -p port_number -u username -w password

    OSS的格式: -s oss -h endpoint -u accessKeyId -w accessKeySecret -b bucketName -p folder_path

    本地存储的格式：-d local_path

-l 包含将要ship的数据库和表的信息的json文件的路径， required
    
    文件格式见下方示例

--no-owner 不把表的所有权设置对应源数据库，否则holo-shipper默认保留表的owner

--no-all-roles 不ship源实例的所有用户，只ship需要的用户（需要ship的表的owner和对表有权限的相关用户）。如果同时设置--no-owner 和 --no-priv 那么不会ship任何用户。holo-shipper默认会ship所有用户

--no-guc 不同步源数据库的GUC参数。否则holo-shipper默认同步GUC参数

--no-ext 不同步源数据库安装的extension。否则holo-shipper默认同步extension

--no-priv 不同步源表的相关权限，否则默认会同步表的权限  
如果源数据库是spm/slpm模型，同步权限的话会在目的数据库开启spm/slpm。如果两边的权限模型不一样(一个是spm一个是slpm,源数据库不是spm/slpm但是目的数据库是spm/slpm)，需要使用--no-priv 和 --no-owner

--no-data 只创建数据库、表结构等，不同步表的数据

--no-foreign 不迁移外部表的DDL, 不添加这个选项的话默认迁移满足shipList条件的外部表的DDL

--no-view 不迁移视图，不添加这个选项的话默认迁移满足shipList条件的视图


示例：

将一个Holo实例中的表dump到本地存储，并且不保留GUC参数信息
```
$ java -jar holo-shipper.jar -s holo -h xx.xx.xx.xx -p xxxx -u username -w password -d local_storage_path -l ship_list_json_path --no-guc
```

将一个Holo实例中的表ship到另一个Holo实例中并且不保留表的owner信息
```
$ java -jar holo-shipper.jar -s holo -h xx.xx.xx.xx -p xxxx -u username -w password -d holo -h xx.xx.xx.xx -p xxxx -u username2 -w password2  -l ship_list_json_path --no-owner
```

将一个Holo实例中的某些表ship到另一个Holo实例中，保留源表owner和权限，不同步与这些表无关的用户，不同步源数据库的guc参数和extension
```
$ java -jar holo-shipper.jar -s holo -h xx.xx.xx.xx -p xxxx -u username -w password -d holo -h xx.xx.xx.xx -p xxxx -u username2 -w password2  -l ship_list_json_path --no-all-roles --no-guc --no-ext
```

将一个之前dump到本地的备份restore到另一个Holo实例
```
$ java -jar holo-shipper.jar -s local_storage_path -d holo -h xx.xx.xx.xx -p xxxx -u username -w password -l ship_list_json_path
```

将一个Holo实例中的表备份到OSS存储（bucket为testBucket, 存储的根目录为 testDump/）,并保留所有信息
```
$ java -jar holo-shipper.jar -s holo -h xx.xx.xx.xx -p xxxx -u username -w password -d oss -h endpoint -u accessKeyId -w accessKeySecret -b testBucket -p testDump/ -l ship_list_json_path
```

将一个之前在OSS的备份restore到另一个Holo实例
```
$ java -jar holo-shipper.jar -s oss -h endpoint -u accessKeyId -w accessKeySecret -b testBucket -p testDump/ -d holo -h xx.xx.xx.xx -p xxxx -u username -w password -l ship_list_json_path
```

json 文件格式：
- 首先为一个JSONArray, array中每一个JSONObject代表一个数据库。
- 每个数据库的JSONObject包含：
   - key: "dbName" value: 数据库名称 (String), required。
   - key: "shipList" value: JSONObject(其中每个key为schema名，value为要ship的表名的list), required。在shipList中的表和他们的子表（且不在blackList中）都会被ship, 选择这个schema中的所有表用“*”
   - key: "blackList" value: JSONObject(其中每个key为schema名，value为不要ship的表名的list), optional。在blackList中的表和他们的子表不会被ship
   - key: "sinkDB" value: 目的地数据库名称(String), optional. 不提供的话默认和“dbName"相同。 当源和目的实例为同一个holo实例时可以视为将表从 "dbName"移到"sinkDB"
   - key: "schemaMapping" value: JSONObject(其中每个key为源schema名， value为在目标的schema名)， optional。如果需要改变schema就在这里指定，如果不指定默认schema不变
   - key: "tgMapping" value: JSONObject(其中每个key为源table group名， value为在目标的table group名)， optional。如果需要改变table group就在这里指定，如果不指定将默认使用目标库的默认table group

example.json
   ```
   [
    {
        "dbName": "DB1",
        "shipList": {
            "schema1": ["*"],
            "schema2": ["table1", "table2"]
        },
        "blackList": {
            "schema1": ["table3"]
        }
    },
    {
        "dbName": "DB2",
        "shipList": {
            "schema3": ["*"],
            "schema4": ["table4", "table5"]
        },
        "schemaMapping": {
            "schema4": "schema5"
        },
        "tgMapping": {
            "tablegroup1": "tablegroup2"
        }
    },
    {
        "dbName": "DB3",
        "shipList": {
            "public": ["*"]
        },
        "sinkDB" : "DB3_backup"
    }
   ]
   ```
Explaination:  
要ship的数据库为DB1，DB2和DB3  
DB1中schema1的所有表（除了table3和他的子表）和schema2的table1,table2和他们的子表将被ship  
DB2中schema3的所有表，和schema4的table4,table5和他们的子表将被ship
DB2中schema4下的表会变为schema5下，i.e. schema4.table4在destination会是schema5.table4. （如果源是slpm模式并且ship时选择保留权限，那么某个用户在源schema4拥有的权限会ship去目的地schema5中）  
将源实例中DB3 public schema下的所有表移到目标实例的DB3_backup public schema下

## 生成jar包
在holo-shipper文件夹下执行
```
$ mvn package
```
holo-shipper/target/holo-shipper-1.2.x.jar 即为生成的可执行jar包
