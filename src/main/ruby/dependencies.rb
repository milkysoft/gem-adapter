# frozen_string_literal: true
require 'rubygems/package'

class Dependencies

    def initialize(val)
        @val = val
    end

    def dependencies()
        resgems = []
        @val.split.each do |gem|
            resdep = []
            spec = Gem::Package.new(gem).spec
            deps = spec.dependencies
            deps.each do |item|
                if item.type == :runtime
                    resdep.push([item.name, item.requirements_list()[0]])
                end
            end
            resgems.push({:name => spec.name, :number=>spec.version.version, :platform=>spec.original_platform, :dependencies=>resdep})
        end
        return Marshal.dump(resgems)
    end
end
