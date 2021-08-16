# frozen_string_literal: true
require 'rubygems/indexer.rb'

class MetaRunner

    def initialize(val)
        info(val)
        dir1 = File.dirname(val)
        dir2 = File.expand_path("..", dir1)
        Gem::Indexer.new(dir2, {build_modern:true}).generate_index
    end

    def info(val)
        spec = Gem::Package.new(val).spec()
        metas = []
        metafiles = ['latest_specs.4.8', 'specs.4.8']
        metafiles.each do |f|
            metas = []
            found = false
            if File.file?(f)
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
