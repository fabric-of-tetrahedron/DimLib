# DimLib

这是一个Fabric模组，可以：

* 在服务器运行时或初始化时添加和移除维度。
* 当维度发生变化时，如果客户端安装了该模组，则将维度信息同步到客户端。
* 通过配置或代码允许抑制"不支持使用实验性设置的世界"警告。

对于动态维度功能，该模组在服务器端是必需的，但在客户端不是必需的。如果客户端没有安装该模组，维度ID的命令补全将不会在维度变化时更新。

如果你只想要禁用警告的功能，该模组仅在客户端需要安装。

所有API都在`DimensionAPI`类中。你可以参考javadoc。

### 命令

#### `/dims add_dimension <新维度ID> <预设>`

基于预设动态添加一个新维度。

示例：`/dims add_dimension "aaa:ccc" void`

目前，唯一内置的预设是`void`。其他模组可以通过API添加预设。

#### `/dims clone_dimension <模板维度> <新维度ID>`

动态添加一个新维度。新维度的维度类型和区块生成器将与`模板维度`相同。

此命令仅克隆维度类型和世界生成器。它不会克隆世界中的内容（方块、实体等）。

示例：`/dims clone_dimension minecraft:overworld "aaa:bbb"`将动态添加维度`aaa:bbb`，其世界生成与主世界相同。

#### `/dims remove_dimension <维度>`

动态移除一个维度。

此命令不会删除该维度的世界存档。

#### `/dims view_dim_config <维度>`

显示维度的配置。包括维度类型ID和区块生成器配置。

### 配置依赖

在`repositories`中添加以下内容

```gradle
maven { url 'https://jitpack.io' }
```

在`dependencies`中添加以下内容

```gradle
modApi("com.github.iPortalTeam:DimLib:${dimlib_version}") {
    exclude(group: "net.fabricmc.fabric-api")
}
```