package com.zwh.compiler;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.zwh.annotation.ARouter;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * @Author neil
 * @Description TODO
 * @Date 2019-10-20 11:57
 */


// AutoService则是固定的写法，加个注解即可
// 通过auto-service中的@AutoService可以自动生成AutoService注解处理器，用来注册
// 用来生成 META-INF/services/javax.annotation.processing.Processor 文件
@AutoService(Processor.class)
// 允许/支持的注解类型，让注解处理器处理
@SupportedAnnotationTypes({"com.zwh.annotation.ARouter"})
// 指定JDK编译版本
@SupportedSourceVersion(SourceVersion.RELEASE_7)
// 注解处理器接收的参数
@SupportedOptions("content")
public class ARouterProcessor extends AbstractProcessor {

    // 操作Element工具类 (类、函数、属性都是Element)
    private Elements elementUtils;

    // type(类信息)工具类，包含用于操作TypeMirror的工具方法
    private Types typeUtils;

    // Messager用来报告错误，警告和其他提示信息
    private Messager messager;

    // 文件生成器 类/资源，Filter用来创建新的源文件，class文件以及辅助文件
    private Filer filer;

    @Override   // 该方法主要用于一些初始化的操作，通过该方法的参数ProcessingEnvironment可以获取一些列有用的工具类
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        elementUtils = processingEnvironment.getElementUtils();
        messager = processingEnvironment.getMessager();
        filer = processingEnvironment.getFiler();
        // 通过ProcessingEnvironment去获取build.gradle传过来的参数
        String content = processingEnvironment.getOptions().get("content");
        // 有坑：Diagnostic.Kind.ERROR，异常会自动结束，不像安卓中Log.e那么好使
        messager.printMessage(Diagnostic.Kind.NOTE, content);
    }

    @Override  //允许/支持的注解类型，让注解处理器处理
    public Set<String> getSupportedAnnotationTypes() {
        return super.getSupportedAnnotationTypes();
    }

    @Override  //指定JDK编译版本
    public SourceVersion getSupportedSourceVersion() {
        return super.getSupportedSourceVersion();
    }

    @Override  //注解处理器接收的参数
    public Set<String> getSupportedOptions() {
        return super.getSupportedOptions();
    }

    /**
     * 相当于main函数，开始处理注解
     * 注解处理器的核心方法，处理具体的注解，生成Java文件
     *
     * @param set              使用了支持处理注解的节点集合（类 上面写了注解）
     * @param roundEnvironment 当前或是之前的运行环境,可以通过该对象查找找到的注解。
     * @return true 表示后续处理器不会再处理（已经处理完成）
     */
    @Override  //
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (set.isEmpty()) return false;
        return processByJavaPoet(roundEnvironment);

    }

    private boolean processByWriter(RoundEnvironment roundEnvironment){
        // 获取所有带ARouter注解的 类节点
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(ARouter.class);
        Element firstElement = (Element) elements.toArray()[0];
        // 最终想生成的类文件名
        String finalClassName = Constants.PATH_FILE_NAME;
        String packageName = elementUtils.getPackageOf(firstElement)
                .getQualifiedName().toString();
            try {
                // 创建一个新的源文件（Class），并返回一个对象以允许写入它
                JavaFileObject sourceFile = filer.createSourceFile(packageName + "." + finalClassName);
                // 定义Writer对象，开启写入
                Writer writer = sourceFile.openWriter();
                // 设置包名
                writer.write("package " + packageName + ";\n");
                // 导包
                writer.write("import java.util.HashMap;\n");
                writer.write("import java.util.Map;\n\n");

                writer.write("public class " + finalClassName + " {\n");

                writer.write("public static Map<String, Class> loadPath() {\n");
                writer.write("Map<String, Class> pathMap = new HashMap<>();\n");
                // 遍历所有类节点
                for (Element element : elements) {
                    // 获取简单类名
                    String className = element.getSimpleName().toString();
                    messager.printMessage(Diagnostic.Kind.NOTE, "被注解的类有：" + className);
                    // 获取类之上@ARouter注解的path值
                    ARouter aRouter = element.getAnnotation(ARouter.class);
                    writer.write("pathMap.put(\""+aRouter.path()+"\", "+className+".class);\n");
                }
                writer.write("return pathMap;\n");
                writer.write("}\n}");
                // 最后结束别忘了
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        return true;
    }

    private boolean processByJavaPoet(RoundEnvironment roundEnvironment){
        try {
            TypeName methodReturns = ParameterizedTypeName.get(
                    ClassName.get(Map.class), // Map
                    ClassName.get(String.class), // Map<String,
                    ClassName.get(Class.class) // Map<String, RouterBean>
            );
            // 构建方法体
            MethodSpec.Builder methodBuidler = MethodSpec.methodBuilder("loadPath") // 方法名
                    .addModifiers(Modifier.PUBLIC,Modifier.STATIC)
                    .returns(methodReturns);

            // 遍历之前：Map<String, Class> pathMap = new HashMap<>();
            methodBuidler.addStatement("$T<$T, $T> $N = new $T<>()",
                    ClassName.get(Map.class),
                    ClassName.get(String.class),
                    ClassName.get(Class.class),
                    Constants.PATH_PARAMETER_NAME,
                    HashMap.class);

            // 获取所有带ARouter注解的 类节点
            Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(ARouter.class);
            String packageName = elementUtils.getPackageOf((Element) elements.toArray()[0])
                            .getQualifiedName().toString();

            // 遍历所有类节点
            for (Element element : elements) {
                // 获取简单类名
                String className = element.getSimpleName().toString();
                messager.printMessage(Diagnostic.Kind.NOTE, "被注解的类有：" + className);
                // 最终想生成的类文件名
                ARouter aRouter = element.getAnnotation(ARouter.class);
                methodBuidler.addStatement(
                        "$N.put($S, $T.class)",
                        Constants.PATH_PARAMETER_NAME, // pathMap.put
                        aRouter.path(), // "/app/MainActivity"
                        ClassName.get((TypeElement) element)// MainActivity
                );
            }
            // 遍历之后：return pathMap;
            methodBuidler.addStatement("return $N", Constants.PATH_PARAMETER_NAME);
            // 最终生成的类文件名
            String finalClassName = Constants.PATH_FILE_NAME;
            messager.printMessage(Diagnostic.Kind.NOTE, "APT生成路由Path类文件：" +
                    packageName + "." + finalClassName);

            // 生成类文件：ARouter$$Path$$app
            JavaFile.builder(packageName, // 包名
                    TypeSpec.classBuilder(finalClassName) // 类名
                            .addModifiers(Modifier.PUBLIC) // public修饰符
                            .addMethod(methodBuidler.build()) // 方法的构建（方法参数 + 方法体）
                            .build()) // 类构建完成
                    .build() // JavaFile构建完成
                    .writeTo(filer);

            return true;
        }catch (Exception e){
            messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
            return false;
        }
    }
}
