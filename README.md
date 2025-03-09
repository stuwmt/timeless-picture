# 智能协同云图库

```
网址：https://picture.abcsummer.site
测试账号:admin123 admin123
```

技术栈：ssm，springboot，mysql，redis，cos，satoken

功能：

管理员：用户管理：删除、编辑用户

​				图片管理：编辑、删除、审核用户上传至公共图库的图片、批量创建图片

​				空间管理：可对公共图库、用户空间进行管理分析（echart）

用户：上传图片、编辑图片、创建私人空间、创建团队空间

1. 图片
   1. 图片上传方式：直接上传、url上传
   2. 图片批量编辑、多维度搜索（图片大小、颜色、格式等）
   3. 图片以图搜图，使用BingSearch API
   4. 使用阿里百炼的图片扩充模型，可对图片进行AI扩图
   5. 管理员可批量创建（使用jsoup批量爬取）
2. 空间
   1. 管理员可创建不同等级的空间，对应不同的图片上传数量和大小，用户只能创建普通空间
   2. 公共图库：显示所有审核过的图片，图片所有者可进行编辑，其他人可进行查看、下载和分享
   3. 私人空间：存放用户自己上传的图片
3. 空间成员
   1. 用户可创建团队空间，并可邀请其他成员，赋予成员不同的角色：管理员、编辑者、浏览者
   2. 空间创建者对本空间进行数据分析
   3. 使用websocket支持团队成员进行实时编辑
