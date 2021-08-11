# frozen_string_literal: true
require 'updatemeta.rb'

class MetaRunner

    def initialize(val)
        Ex.new(val).info()
        puts val
    end
end
