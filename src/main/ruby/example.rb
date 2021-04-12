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

    def sum(x)
        return @val + x
    end
end