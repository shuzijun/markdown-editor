# 自定义样式

* [English Document](https://github.com/shuzijun/markdown-editor/blob/main/assets/CustomStyle.md)

- [中文文档](#简介)

## 简介

渲染的编辑器是一个html页面，这个页面默认加载的是jar包内的资源和样式，只适配了默认的暗黑主题和白色主题，如果是使用的其他主题配色可能很丑。

插件提供了一种加载外部主题的方式，可以加载固定目录下的html页面和样式。通过 *File | Settings | Tools | Markdown Editor*  找到插件的配置页，可以看到一个模板路径和同步按钮。

点击同步按钮同步默认的页面和主题到路径下，目前这个路径是插件的安装路径，暂时不可修改，并且每次重新安装插件会被清空，注意备份。

接下来就可以需要一些使用chrome的开发者工具和css的知识了，在编辑器右键会出现*open DevTools* ，打开后通过选择元素，查看样式，看到目前加载样式类型，然后根据自己的需求进行修改。

下面介绍一下文件加下的结构。

## template/default.html

这是渲染编辑器的主要页面，所有的配置项都在这个文件里，相关配置都可以在[vditor](https://github.com/Vanessa219/vditor)查到，如果有更高的自定义需求，可以查阅配置进行修改。

与主题相关的应该配置为 *"theme": darcula ? "dark" : "light"*,*"current": darcula ? "idea-dark" : "idea-light"*

其中darcula变量未当前idea是否是使用的darcula主题,idea-dark与idea-light主题为默认加载的主题。

## vditor/content-theme

这个目录是存放主题的地方。如果有自定完整主题的需求，可以新增一个自己的css文件，并且修改default.html中主题名称，如果只是想修改部分样式，可以通过下面的userStyle.css配置

## vditor/userStyle.css

该配置主要是提供用户自定义的样式，如果有部分修改样式的需要，可以通过开发者工具定位到元素，然后找到要修改的样式，在这个文件内进行重写。

## 最后

vditor的主题没有找到相关文档，只能通过浏览器工具进行定位重写了。

欢迎各位将重写的过程，重写元素的含义和重写的主题和文件进行共享！:smile:
