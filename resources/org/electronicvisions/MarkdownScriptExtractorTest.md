# Script extractor test

This is a sample markdown file for testing jenlib's MarkdownScriptExtractor.

Let's start with a very simple shell script:
```shell
pwd
```

Let's mix in some other block
```groovy
def helloWorld(){
    println("Hello World")
}
```

How about some more newlines?
```shell
echo "Hello world"

pwd
```

## Failure Tests

These should fail, individually:

```fail-0
false
true
```

```fail-1
false | true
```

```fail-2
echo ${MUSTNOTEXIST_5709142e}
```
