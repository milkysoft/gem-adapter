# frozen_string_literal: true
require 'rubygems/package'

class Dependencies

    def initialize(val)
        resgems = []
        val.split.each do |gem|
            resdep = []
            spec = Gem::Package.new(gem).spec
            deps = spec.dependencies
            deps.each do |item|
                if item.type == :runtime
                    resdep.append([item.name, item.requirements_list()[0]])
                end
            end
            resgems.append({:name => spec.name, :number=>spec.version.version, :platform=>spec.original_platform, :dependencies=>resdep})
        end
        return Marshal.dump(resgems)
    end
end