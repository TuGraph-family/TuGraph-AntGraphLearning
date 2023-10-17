# 如何贡献代码

## 提交 Bug

如果您在AGL中发现了一个 非安全问题相关的 Bug, 请先到 Issues 中搜索，以防止该 Bug 已被提交。
如果找不到，请创建一个Issue来描述这个Bug

## 提交安全性问题

如果您在AGL中发现了一个 安全性问题，请不要公开，通过邮件联系 owner, 并在邮件中详细描述该安全问题。

## 解决现有问题

通过查看仓库 Issues 列表发现需要处理的问题信息，可以尝试解决其中的某个问题。

## 如何设置开发环境

请参考 [安装指南](install.md)

## 代码规范

### Python

#### Code Style

python 的 code style 总体上要求符合pep8的标准.

如果使用 PyCharm开发，可以通过 [BlackConnect](https://black.readthedocs.io/en/stable/integrations/editors.html) 插件进行format.

#### Docstring

使用 Google style format.

如果使用 Pycharm 开发，可以在 Preferences -> Tools -> Python Integrated Tools -> Docstrings 进行配置

### C++

#### Code style

使用 Google Style.

如果使用 Clion 开发 通过如下方式进行配置和 format [link](https://www.jetbrains.com/help/clion/predefined-code-styles.html)

### Java

#### Code style

使用 Google Style.

如果使用 Intellij 开发，通过如下方式进行进行配置和 format：

(1)
下载 [intellij-java-google-style.xml](https://github.com/google/styleguide/blob/gh-pages/intellij-java-google-style.xml)
文件

(2) 在 Intellij 中, Settings -> Editor -> Code Style -> import schemes-> intellij idea code style xml 进行配置

## Issue 与 PR

对于图采样流程，图数据格式（GraphFeature)， 为保证不同版本的一致性，我们一般不会轻易修改，因此可以先通过 issue 等方式提交问题进行讨论。

对于功能优化，功能扩展，Bug fix 等方面的问题, 我们非常欢迎进行相应的修改。

## 开发流程

* 切换到你的开发分支
  ```
  git checkout -b your-branch
  ....
  git add xxx
  git commit -m "xxx"
  ```

* 开发你的功能

* 添加单测

  单测执行方式

  ```bash
  # c++ 单测
  bash ./script/cpp_ut.sh
  # python 单测
  bash ./script/python_ut.sh
  # java 单测
  mvn clean package # 编译并执行单测
  ```

* 提交PR

* 处理冲突

  ```
  git checkout your-branch
  git rebase master # 确保本地 master 是最新的
  ```
* Code review

  你提交的代码需要通过 code review 才能合入到 master, 请耐心等待。
  我们将分配相关同学进行 code review.
  如果相关同学4个工作日仍然没有回应你的PR，请在你的PR中 @ 相关同学

  code review的相关评论会直接贴到相关的 PR 或 issue 中。 如果你觉得相关建议是合理的，请更新你的代码。

* 合入到 master

  code review 通过后，我们会安排新的同学进一步review, 确保每个 PR 至少有两个同意后才能合入到主线。
  这个过程中可能也会出现一些需要修改的意见，请耐心修改。
  都通过后，PR将会合入到 master.
