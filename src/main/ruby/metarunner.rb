# frozen_string_literal: true
require 'updatemeta.rb'
require 'rubygems/indexer.rb'

class MetaRunner

    def initialize(val)
        Ex.new(val).info()
        dir1 = File.dirname(val)
        dir2 = File.expand_path("..", dir1)
        puts 'Building index in:     ' + dir2
        Gem::Indexer.new(dir2, {build_modern:true}).generate_index
        FileUtils.cp('latest_specs.4.8', dir2)
        FileUtils.cp('latest_specs.4.8.gz', dir2)
        FileUtils.cp('specs.4.8', dir2)
        FileUtils.cp('specs.4.8.gz', dir2)
    end
end
