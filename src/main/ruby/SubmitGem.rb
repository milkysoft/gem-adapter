require 'java'
require 'builder'
require 'rubygems/indexer.rb'
# @todo #32:120min Gem submission implementation.
#  The implementation must receive the .gem file, unzip it, and update specs files. As a result,
#  files become available for downloading.
class SubmitGem
  java_import com.artipie.http.rs.RsWithStatus
  java_import com.artipie.http.rs.RsStatus
  java_import org.slf4j.LoggerFactory
  include com.artipie.http.Slice

  @@log = LoggerFactory::getLogger("com.artipie.gem.SubmitGem")

  def initialize(storage)
    @storage = storage
    idx = "temp-gem-index"
    gems = File.join(idx, "gems")
    Dir.mkdir(idx) unless File.exists?(idx)
    Dir.mkdir(gems) unless File.exists?(gems)
    @indexer = Gem::Indexer.new(idx, { build_modern: true })
  end

  def response(line, headers, body)
    @@log.debug("Requested #{line}")
    RsWithStatus.new(RsStatus::NOT_IMPLEMENTED)
  end
end