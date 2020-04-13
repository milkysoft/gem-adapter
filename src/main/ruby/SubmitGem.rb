require 'java'
# @todo #32:120min Gem submission implementation.
#  The implementation must receive the .gem file, unzip it, and update specs files. As a result,
#  files become available for downloading.
class SubmitGem
  java_import com.artipie.http.rs.RsWithStatus
  java_import com.artipie.http.rs.RsStatus
  include com.artipie.http.Slice
  def response(line, headers, body)
    RsWithStatus.new(RsStatus::NOT_IMPLEMENTED)
  end
end