#!/usr/bin/env ruby
# A standalone .rb file — Ruby Portable runs it with the portable interpreter.
# Click the green ▶ in the gutter.

=begin
A =begin/=end block comment, which only counts at column 0.
=end

class Greeter
  GREETINGS = %w[Hello Xin\ chào Hej].freeze

  def initialize(name:, times: 3)
    @name  = name          # instance variable
    @times = times
    @@made = (@@made ||= 0) + 1
  end

  # Symbols, string interpolation, and a predicate method.
  def shouty? = @name == @name.upcase

  def greet
    @times.times.map { |i| "#{i + 1}. #{GREETINGS.sample}, #{@name}!" }
  end

  def self.made = @@made
end

# The hard case: `/` here opens a REGEX, but in `total / count` it is division.
SLUG = /\A[a-z0-9\-]+\z/i

report = <<~TEXT
  A squiggly heredoc keeps its own indentation
  and runs until the terminator line.
TEXT

total, count = 10, 4
average = total / count           # division, not a regex
initial = ?R                      # ?x is a one-char string, not a ternary

puts Greeter.new(name: "Ruby", times: 2).greet
puts "slug ok: #{SLUG.match?('ruby-portable')}"
puts "average: #{average}, hex: #{0xFF}, big: #{1_000_000}"
puts "initial: #{initial}, greeters made: #{Greeter.made}"
puts report
