# frozen_string_literal: true
require 'rubygems/indexer.rb'

class AstoUpdater
    def index(directory)
        Gem::Indexer.new(directory,{ build_modern:true }).generate_index
    end
end