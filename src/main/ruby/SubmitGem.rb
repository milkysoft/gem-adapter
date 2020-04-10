require 'java'
class SubmitGem
  include com.artipie.http.Slice
  def response(line, headers, body)
    com.artipie.http.rs.RsWithStatus.new(com.artipie.http.rs.RsStatus.NOT_IMPLEMENTED)
  end
end