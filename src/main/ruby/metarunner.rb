# frozen_string_literal: true
require 'updatemeta.rb'
require 'rubygems/indexer.rb'

class MetaRunner

    def initialize(val)
        Ex.new(val).info()
        dir1 = File.dirname(val)
        dir2 = File.expand_path("..", dir1)
        puts 'Directory: ' + File.expand_path(File.dirname(File.dirname(__FILE__)))
        Gem::Indexer.new(dir2, {build_modern:true}).generate_index
    end
end
