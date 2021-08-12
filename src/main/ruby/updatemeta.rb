# frozen_string_literal: true
require 'rubygems'
require 'rubygems/package'
require 'time'
require 'java'

class Ex
    def initialize(val)
        @val = val
    end

    def info()
        spec = Gem::Package.new(@val).spec()
        metas = []
        metafiles = ['latest_specs.4.8', 'specs.4.8']
        metafiles.each do |f|
            metas = []
            found = false
            if File.file?(f) and File.size(f) > 10
                content = File.open(f).read
                metas = Marshal.load(content)
                metas.each do |item|
                    if item[0] == spec.name && item[1].version == spec.version.version
                        found = true
                    end
                end
            end
            if found == false
                metas.push([spec.name, Gem::Version.create(spec.version.version), "ruby"])
                data = Marshal.dump(metas)
                File.write(f, data)
                Zlib::GzipWriter.open(f + '.gz') do |gz|
                    gz.write data
                end
            end
        end
    end
end
