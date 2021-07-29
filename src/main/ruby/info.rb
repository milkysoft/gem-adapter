# frozen_string_literal: true
require 'rubygems'
require 'rubygems/package'
require 'time'
require 'tmpdir'
require 'digest'
require 'json'
require 'java'

class Ex
    include com.artipie.gem.IRubyInfo

    def initialize(val)
        @val = val
    end

    def info()
        sha = Digest::SHA2.new
        File.open(@val) do |f|
            while chunk = f.read(256)
                sha << chunk
            end
        end
        spec = Gem::Package.new(@val).spec
        authors = ""
        spec.authors.each do |author|
            if authors.length > 0
                authors = authors + ", "
            end
            authors = authors + author
        end
        deps = spec.dependencies
        development = []
        runtime = []
        deps.each do |item|
            if item.type == :runtime
                runtime.push({'name' => item.name, 'requirements' => item.requirements_list()[0]})
            else
                development.push({'name' => item.name, 'requirements' => item.requirements_list()[0]})
            end
        end
        info = {'name' => spec.name, 'version' => spec.version.version, 'sha' => sha.hexdigest, 'info' => spec.description, 'authors' => authors, 'homepage_uri' => spec.homepage, 'dependencies' => {'development' => development, 'runtime' => runtime}}
        return info.to_json
    end
end