require 'java'
require 'builder'
# @todo #32:120min Gem submission implementation.
#  The implementation must receive the .gem file, unzip it, and update specs files. As a result,
#  files become available for downloading.
class SubmitGem
  java_import com.artipie.http.rs.RsWithStatus
  java_import com.artipie.http.rs.RsStatus
  java_import org.slf4j.LoggerFactory
  include com.artipie.http.Slice
  @@log = LoggerFactory::getLogger("com.artipie.gem.SubmitGem")
  def response(line, headers, body)
    @@log.debug("Requested #{line}")
    RsWithStatus.new(RsStatus::NOT_IMPLEMENTED)
  end
end