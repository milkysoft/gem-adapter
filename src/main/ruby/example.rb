# frozen_string_literal: true
require 'rubygems'
require 'rubygems/package'
require 'time'
require 'tmpdir'
require 'java'

class Ex
    include com.artipie.gem.Example

    def initialize(val)
        @val = val
    end

    def dependencies()
        resdep = []
        spec = Gem::Package.new(@val).spec
        deps = spec.dependencies
        deps.each do |item|
            if item.type == :runtime
                resdep.append([item.name, item.requirements_list()[0]])
            end
        end
        return Marshal.dump([{:name => spec.name, :number=>spec.version.version, :platform=>spec.original_platform, :dependencies=>resdep}])
    end
end