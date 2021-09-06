package com.artipie.gem.ruby;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jruby.Ruby;
import org.jruby.RubyObject;
import org.jruby.RubyRuntimeAdapter;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.Variable;

public class Dependencies {
    /**
     * Ruby runtime.
     */
    private final RubyRuntimeAdapter runtime;

    /**
     * Ruby interpreter.
     */
    private final Ruby ruby;

    /**
     * New Ruby object JSON converter.
     * @param ruby Is Ruby system
     */
    public Dependencies(final Ruby ruby) {
        this.ruby = ruby;
        this.runtime = JavaEmbedUtils.newRuntimeAdapter();
    }

    /**
     * Create JSON info for gem.
     * @param gempath Full path to gem file or null
     * @return JsonObjectBuilder result
     */
    public JsonObject getinfo(final Path gempath) {
        final List<Variable<Object>> vars = this.getSpecification(gempath)
            .getVariableList();
        final JsonObjectBuilder obj = Json.createObjectBuilder();
        for (final Variable<Object> var : vars) {
            String name = var.getName();
            if (name.charAt(0) == '@') {
                name = var.getName().substring(1);
            }
            if (var.getValue() != null) {
                obj.add(name, var.getValue().toString());
            }
        }
        return obj.build();
    }

    /**
     * Create marshaled binary info for gem.
     * @param gempath Full path to gem file
     * @return String result
     */
    public String getPathDependency(final Path gempath) {
        String res = "";
        res = res.concat("{");
        final List<Variable<Object>> vars = this.getSpecification(gempath)
            .getVariableList();
        for (final Variable<Object> var : vars) {
            final String name = var.getName();
            if (name.equals("@dependencies")) {
                res = res.concat(":name=>\"").concat(Dependencies.getGemName(vars))
                    .concat("\", :number=>\"").concat(Dependencies.getGemVersion(vars))
                    .concat("\"");
                res = res.concat(", :platform=>\"ruby\", :dependencies=>[");
                final String val = var.getValue().toString();
                final String[] dependencies = val.substring(1, val.length() - 1).split(",");
                for (final String dependency : dependencies) {
                    final String[] result = Dependencies.parseDependency(dependency);
                    if (result[0].length() > 0) {
                        res = res.concat("[\"".concat(result[0]).concat("\", \"").concat(result[1])
                            .concat("\"]")
                        );
                    }
                }
                res = res.concat("]");
            }
        }
        return res.concat("}");
    }

    /**
     * Create marshaled binary info for gem.
     * @param gempaths Full paths to gem files
     * @return String result
     */
    public byte[] getDependencies(final List<Path> gempaths) {
        String paths = "";
        for (int i=0; i< gempaths.size(); i++) {
            if(paths.length() > 0) {
                paths = paths.concat(" ");
            }
            System.out.println(String.format("Current path: %s", gempaths.get(i).toString()));
            paths = paths.concat(gempaths.get(i).toString());
        }
        final String script;
        try {
            script = IOUtils.toString(
                Dependencies.class.getResourceAsStream("/dependencies.rb"),
                StandardCharsets.UTF_8
            );
            IRubyObject recvr = this.runtime.eval(this.ruby, script);
            IDependencies ex = (IDependencies) JavaEmbedUtils.invokeMethod(
                this.ruby,
                this.runtime.eval(this.ruby, "Dependencies"),
                "new",
                new Object[]{paths},
                IDependencies.class
            );
            org.jruby.RubyString obj = ex.dependencies();
            FileUtils.writeByteArrayToFile(new File("yyyy.data"), obj.getBytes());
            System.out.printf("val=%s\n", obj);
            System.out.println("8776786");
            return obj.getBytes();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "".getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Get Ruby specification for arbitrary gem.
     * @param dependency Ruby specification dependency string
     * @return Two strings for dependency name and version
     */
    private static String[] parseDependency(final String dependency) {
        final String[] res = new String[2];
        res[0] = "";
        res[1] = "";
        String srch = "type=";
        int indexs = dependency.indexOf(srch) + srch.length();
        srch = " name=\"";
        int indexe = dependency.indexOf(srch, indexs);
        if (!":development".equals(dependency.substring(indexs, indexe))) {
            srch = "name=\"";
            indexs = dependency.indexOf(srch) + srch.length();
            indexe = dependency.indexOf("\" ", indexs);
            final String depname = dependency.substring(indexs, indexe);
            srch = "requirements=\"";
            indexs = dependency.indexOf(srch, indexe) + srch.length();
            indexe = dependency.indexOf("\">", indexs);
            final String depver = dependency.substring(indexs, indexe);
            res[0] = depname;
            res[1] = depver;
        }
        return res;
    }

    /**
     * Get Ruby specification for arbitrary gem.
     * @param gempath Full path to gem file or null
     * @return RubyObject specification
     */
    private RubyObject getSpecification(final Path gempath) {
        return (RubyObject) this.runtime.eval(
            this.ruby, String.format(
                "require 'rubygems/package.rb'\nGem::Package.new('%s').spec", gempath.toString()
            )
        );
    }

    /**
     * Get Ruby specification for arbitrary gem.
     * @param data Full path to gem file or null
     * @return RubyObject specification
     */
    private RubyObject getMarshal(final String data) {
        return (RubyObject) this.runtime.eval(
            this.ruby, String.format(
                "Marshal.dump('%s')", data
            )
        );
    }

    /**
     * Get Ruby gem from Path.
     * @param vars List of Variables
     * @return Gem name with version
     */
    private static String getGemVersion(final List<Variable<Object>> vars) {
        String res = "";
        for (final Variable<Object> var : vars) {
            final String name = var.getName();
            if (name.equals("@version")) {
                res = var.getValue().toString();
                break;
            }
        }
        return res;
    }

    /**
     * Get Ruby gem from Path.
     * @param vars List of Variables
     * @return Gem name with version
     */
    private static String getGemName(final List<Variable<Object>> vars) {
        String res = "";
        for (final Variable<Object> var : vars) {
            final String name = var.getName();
            if (name.equals("@name")) {
                res = var.getValue().toString();
                break;
            }
        }
        return res;
    }
}
